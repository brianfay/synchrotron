(ns synchrotron.ugen
  (:require [clojure.set :refer [union]]
            [com.stuartsierra.dependency :as dep]
            [synchrotron.synthdef :as synthdef]
            [synchrotron.scsynth :as scsynth]))

(defn count* [coll] (if (sequential? coll) (count coll) 0))

(defonce current-id (atom 1))

(defonce ugens-in-current-synthdef (atom []))

(defn next-id [] (swap! current-id inc))

(defn first-keyword-index [v]
  (loop [n 0
         args v]
    (when-let [arg (first args)]
      (if (keyword? arg)
        n
        (recur (inc n) (rest args))))))

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
      (throw (js/Error (str "Can't set " dup-arg " as a named arg; already set as an ordered arg"))))
    ;;don't allow args that the ugen doesn't recognize
    (when-let [bad-arg (some
                        #(when-not (contains? (set default-arg-names) %) %)
                        (map first (partition 2 kwargs)))]
      (throw (js/Error (str "invalid arg: " bad-arg))))))

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
  (if-let [k-idx (first-keyword-index args)]
    (let [ordered-args (take k-idx args)
          kwargs (drop k-idx args)
          kwargs-map (reduce (fn [m [k v]] (assoc m k v)) {} (partition 2 kwargs))]
      (assert-valid-args! ordered-args kwargs default-args)
      (->> default-args
           (fill-in-ordered-args ordered-args)
           (fill-in-named-args kwargs-map)))
    (fill-in-ordered-args args default-args)))

(def kw-rate->int-rate {:ir 0
                        :kr 1
                        :ar 2
                        :demand 3})

(defn get-calculation-rate [ugen-template]
  (if-let [rate (:calculation-rate ugen-template)]
    (kw-rate->int-rate rate)
    (kw-rate->int-rate (first (:rates ugen-template)))))

(defn- get-arg
  [args arg-key]
  (let [partitioned-args (partition 2 args)]
    (some (fn [[arg-k arg-v]] (when (= arg-k arg-key) arg-v)) partitioned-args)))

(defn- remove-arg
  [args arg-key]
  (let [partitioned-args     (partition 2 args)
        new-partitioned-list (remove (fn [[arg-k arg-v]] (= arg-k arg-key)) partitioned-args)]
    (flatten new-partitioned-list)))

(defn build-ugen! [id ugen-template args out-index]
  (let [new-ugen {:id id
                  :out-index out-index
                  :calculation-rate (:calculation-rate ugen-template)
                  :ugens (apply merge {id ugen-template} (map :ugens args))
                  :dependencies (set (keep identity (map (fn [arg]
                                                           (when-let [arg-id (get arg :id)]
                                                             [id arg-id])) args)))}]
    (swap! ugens-in-current-synthdef conj new-ugen)
    {:id id :out-index out-index}))

(defn abstract-ugen [ugen-template & args]
  (let [parsed-args (parse-args args (:inputs ugen-template))
        arg-kv-pairs (partition 2 parsed-args)
        arg-vals (map second arg-kv-pairs)]
    ;;if one of the arguments is a list, we expand into multiple ugens (one per each arg in the list)
    (if (some sequential? arg-vals)
      (doall
       (flatten (for [n (range (apply max (map count* args)))]
                  (apply abstract-ugen ugen-template (replace-nth-arg parsed-args n)))))
      (let [id (next-id)
            num-outputs (if (= (:num-outputs ugen-template) :variadic)
                          (get-arg parsed-args :num-channels)
                          (:num-outputs ugen-template))
            ;;remove num-channels for some ugens because it's not an actual ugen input, just a confusing language-side construct
            parsed-args (if (:treat-num-channels-as-input ugen-template)
                          parsed-args
                          (remove-arg parsed-args :num-channels))
            ugen-template (assoc ugen-template
                                 :inputs parsed-args
                                 :id id
                                 :calculation-rate (get-calculation-rate ugen-template)
                                 :num-outputs num-outputs
                                 :outputs (into [] (for [n (range num-outputs)]
                                                     {:calculation-rate (get-calculation-rate ugen-template)})))]
        (doall
         (if (<= num-outputs 1);;TODO: throw an error if a ugen tries to take an out ugen as input
           (build-ugen! id ugen-template args 0)
           (for [out-n (range num-outputs)]
             (build-ugen! id ugen-template args out-n))))))))

;;had a function here called abstract-output-ugen
;;this was for ugens that auto-increment bus number as the macroexpand.
;;didn't get the implementation working with Controls.
;;I sort of prefer being explicit about the buses, but maybe I'll bake that feature into abstract-ugen later
