(ns synchrotron.ugen
  (:require [clojure.set :refer [union]]
            [com.stuartsierra.dependency :as dep]
            [synchrotron.synthdef :as synthdef]
            [synchrotron.scsynth :as scsynth]))

(defn count* [coll] (if (sequential? coll) (count coll) 0))

(defonce current-id (atom 0))

(defonce ugens-in-current-synthdef (atom []))

(defn next-id [] (swap! current-id inc))

(defn first-keyword-index [v]
  (loop [n 0
         args v]
    (when-let [arg (first args)]
      (if (keyword? arg)
        n
        (recur (inc n) (rest args))))))

(defn replace-with-nth
  "For any sequential elements within coll, replace the element with its nth % (length of element) item."
  [coll n]
  (for [element coll]
    (if (sequential? element)
      (nth element (mod n (count element)))
      element)))

(defn replace-nth-arg
  "For any sequential elements within args list, replace the element with its nth % (length of element) item."
  [args n]
  (let [partitioned-args (partition 2 args)
        arg-names (map first partitioned-args)
        arg-vals (map second partitioned-args)
        new-vals (for [element arg-vals]
                   (if (sequential? element)
                     (nth element (mod n (count element)))
                     element))]
    (interleave arg-names new-vals)))

(defn assert-valid-args! [ordered-args kwargs default-args]
  (when (or (not-every? keyword? (map first (partition-all 2 kwargs)))
            (odd? (count kwargs)))
    (throw (js/Error "After first keyword given in ugen arg list, remaining arguments should be key value pairs")))
  ;;don't allow same arg to be set multiple times
  (let [default-arg-names (map first (partition 2 default-args))
        kwarg-names (map first (partition 2 kwargs))
        used-arg-names (take (count ordered-args) default-arg-names)
        dup-arg (some #(get (set kwarg-names) %) (set used-arg-names))]
    (when dup-arg
      (throw (js/Error (str "Can't set " dup-arg " as a named arg; already set as an ordered arg")))))
  ;;don't allow args that the ugen doesn't recognize
  (when-let [bad-arg (some
                      #(when-not (contains? (set default-args) %) %)
                      (map first (partition 2 kwargs)))]
    (throw (js/Error (str "invalid arg: " bad-arg)))))

(defn fill-in-ordered-args
  [ordered-args default-args]
  (let [idxs (range 1 (count default-args) 2)]
    (when (< (count idxs) (count ordered-args)) (throw (js/Error "too many arguments given")))
    (loop [acc default-args
           idxs idxs
           new-args ordered-args]
      (if-let [arg (first new-args)]
        (let [idx (first idxs)]
          (recur (assoc acc idx arg) (rest idxs) (rest new-args)))
        acc))))

(defn fill-in-named-args
  [named-args default-args]
  (reduce (fn [args [k v]]
            (if-let [new-v (get named-args k)]
              (conj args k new-v)
              (conj args k v)))
          []
          (partition 2 default-args)))

(defn parse-args
  "args should be a sequence of some number of ordered args (constants or ugens) followed by some number of
     named args. [220 0.2 :mul 0.3] or [:mul 0.3 :freq 220 :phase 0.2] would be acceptable.
     [:mul 0.3 220 0.2] would not be acceptable; if a named arg occurs somewhere in the sequence,
     all following arguments are assumed to be named as well.
     default-args should be a sequence of named args like [:freq 220 :phase 0.2]."
  [args default-args]
  (let [parsed-args (if-let [k-idx (first-keyword-index args)]
                      (let [ordered-args (take k-idx args)
                            kwargs (drop k-idx args)
                            kwargs-map (reduce (fn [m [k v]] (assoc m k v)) {} (partition 2 kwargs))]
                        (assert-valid-args! ordered-args kwargs default-args)
                        (->> default-args
                             (fill-in-ordered-args ordered-args)
                             (fill-in-named-args kwargs-map)))
                      (fill-in-ordered-args args default-args))]
    ;;TODO: assert no nil args
    ;;convert any ugen maps to a [ugen-id out-index]
    (reduce (fn [acc v] (if (map? v)
                          (conj acc {(get v :id) (get v :out-index)})
                          (conj acc v))) [] parsed-args)))

(def kw-rate->int-rate {:scalar 0
                        :kr 1
                        :ar 2
                        :demand 3})

(defn get-calculation-rate [ugen-template]
  (if-let [rate (:calculation-rate ugen-template)]
    (kw-rate->int-rate rate)
    (kw-rate->int-rate (first (:rates ugen-template)))))

(defn abstract-ugen [ugen-template & args]
  (let [parsed-args (parse-args args (:inputs ugen-template))
        arg-kv-pairs (partition 2 parsed-args)
        arg-vals (map second arg-kv-pairs)]
    ;;if one of the arguments is a list, we expand into multiple ugens (one per each arg in the list)
    ;;TODO support variadic outputs - num-outputs can vary for some ugens using args like num-channels
    ;;TODO mark output
    (if (some sequential? arg-vals)
      (doall
       (flatten (for [n (range (apply max (map count* args)))]
                  (apply abstract-ugen ugen-template (replace-nth-arg parsed-args n)))))
      (let [id (next-id)
            ugen-template (assoc ugen-template
                                 :inputs parsed-args
                                 :id id
                                 :calculation-rate (get-calculation-rate ugen-template)
                                 :outputs (for [n (range (get ugen-template :num-outputs))]
                                            {:calculation-rate (get-calculation-rate ugen-template)}))]
        (doall
         (for [out-n (range (get ugen-template :num-outputs))]
           (let [new-ugen {:id id
                           :out-index out-n
                           :calculation-rate (:calculation-rate ugen-template)
                           :ugens (apply merge {id ugen-template} (map :ugens args))
                           :dependencies (apply union
                                                (set (keep identity (map (fn [arg]
                                                                           (when-let [arg-id (get arg :id)]
                                                                             [id arg-id])) args)))
                                                (map :dependencies args))}]
             (swap! ugens-in-current-synthdef conj new-ugen)
             new-ugen)))))))

(defn abstract-output-ugen
  "This is for ugens that output to buses.
  Name is a bit misleading because these ugens do not have any 'outputs' that can be consumed by other ugens"
  [ugen-template & args]
  ;;TODO assert args are set
  (let [parsed-args (parse-args args (:inputs ugen-template))
        arg-kv-pairs (partition 2 parsed-args)
        arg-vals (map second arg-kv-pairs)
        buses (first arg-vals)
        channels-array (second arg-vals)]
    (if (or (sequential? buses) (sequential? channels-array))
      (let [buses (if (sequential? buses) buses [buses])
            channels-array (if (sequential? channels-array) channels-array [channels-array])]
        (doall (for [bus buses
                     channel-idx (range (count channels-array))]
                 (abstract-output-ugen ugen-template (+ bus channel-idx) (nth channels-array channel-idx)))))
      (let [id (next-id)
            ugen-template (assoc ugen-template
                                 :inputs parsed-args
                                 :id id
                                 :calculation-rate (get-calculation-rate ugen-template)
                                 :outputs []
                                 :num-outputs 0)
            new-ugen {:id id
                      :calculation-rate (:calculation-rate ugen-template)
                      :ugens (apply merge {id ugen-template} (map :ugens args))
                      :dependencies (apply union
                                           (set (keep identity (map (fn [arg]
                                                                      (when-let [arg-id (get arg :id)]
                                                                        [id arg-id])) args)))
                                           (map :dependencies args))}]
        (swap! ugens-in-current-synthdef conj new-ugen)
        new-ugen))))
