(ns synchrotron.synthdef.scratch2
  (:require [clojure.set :refer [union]]
            [com.stuartsierra.dependency :as dep]
            [synchrotron.synthdef :as synthdef]
            [synchrotron.scsynth :as scsynth]))

(defn count* [coll] (if (sequential? coll) (count coll) 0))

(defonce current-id (atom 0))

(defn next-id [] (swap! current-id inc))

(defn assert-valid-args! [ordered-args kwargs default-args]
  ;;don't allow ordered-args after keyword args are being set
  (def ordered-args ordered-args)
  (def kwargs kwargs)
  (def default-args default-args)
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
  ;;loop over "default args"
  ;;split into kv pairs
  ;;if in named args, set to  a value
  (reduce (fn [args [k v]]
            (if-let [new-v (get named-args k)]
              (conj args k new-v)
              (conj args k v)))
          []
          (partition 2 default-args)))

(defn first-keyword-index [v]
  (loop [n 0
         args v]
    (when-let [arg (first args)]
      (if (keyword? arg)
        n
        (recur (inc n) (rest args))))))

(defn parse-args
  "args should be a sequence of some number of ordered args (constants or ugens) followed by some number of
     named args. [220 0.2 :mul 0.3] or [:mul 0.3 :freq 220 :phase 0.2] would be acceptable.
     [:mul 0.3 220 0.2] would not be acceptable; if a named arg occurs somewhere in the sequence,
     all following arguments are assumed to be named as well.
     default-args should be a sequence of named args like [:freq 220 :phase 0.2]."
  [args default-args]
  (def args args)
  (def default-args default-args)
  (let [parsed-args (if-let [k-idx (first-keyword-index args)]
                      (let [ordered-args (take k-idx args)
                            kwargs (drop k-idx args)
                            kwargs-map (reduce (fn [m [k v]] (assoc m k v)) {} (partition 2 kwargs))]
                        (assert-valid-args! ordered-args kwargs default-args)
                        (->> default-args
                             (fill-in-ordered-args ordered-args)
                             (fill-in-named-args kwargs-map)))
                      (fill-in-ordered-args args default-args))]
    ;;convert any ugen maps to a [ugen-id out-index]
    (reduce (fn [acc v] (if (map? v)
                          (conj acc {(get v :id) (get v :out-index)})
                          (conj acc v))) [] parsed-args)))

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

(defn abstract-ugen [ugen-template & args]
  (if (some sequential? args)
    (flatten (for [n (range (apply max (map count* args)))]
               (apply abstract-ugen ugen-template (replace-with-nth args n))))
    (let [id (next-id)]
      (for [out-n (range (get ugen-template :num-outputs))]
        {:id id
         :ugens (apply merge {id ugen-template} (map :ugens args))
         :dependencies (apply union
                              (set (keep identity (map (fn [arg]
                                                         (when-let [arg-id (get arg :id)]
                                                           [id arg-id])) args)))
                              (map :dependencies args))}))))

(defn abstract-ugen [ugen-template & args]
  (let [parsed-args (parse-args args (:inputs ugen-template))
        arg-kv-pairs (partition 2 parsed-args)
        arg-vals (map second arg-kv-pairs)]
    ;;if one of the arguments is a list, we expand into multiple ugens (one per each arg in the list)
    ;;TODO mark output
    ;;TODO calculation-rate
    (if (some sequential? arg-vals)
      (flatten (for [n (range (apply max (map count* args)))]
         (apply abstract-ugen ugen-template (replace-nth-arg parsed-args n))))
      (let [id (next-id)
            ugen-template (assoc ugen-template
                                 :inputs parsed-args
                                 :id id
                                 ;TODO calculation-rate stuff shouldn't be hardcoded
                                 :calculation-rate 2
                                 :outputs (for [n (range (get ugen-template :num-outputs))] {:calculation-rate 2}))]
        (for [out-n (range (get ugen-template :num-outputs))]
          {:id id
           :out-index out-n
           :calculation-rate 1
           :ugens (apply merge {id ugen-template} (map :ugens args))
           :dependencies (apply union
                                (set (keep identity (map (fn [arg]
                                                           (when-let [arg-id (get arg :id)]
                                                             [id arg-id])) args)))
                                (map :dependencies args))})))))

;; :bus [0 1] :channels-array [(sin-osc 220) (sin-osc 330)] 
;;would expand to
;; 0 sin-osc 220
;; 1 sin-osc 330
;; 1 sin-osc 220
;; 2 sin-osc 330
(defn abstract-output-ugen [ugen-template & args]
  ;;TODO assert args are set
  (let [parsed-args (parse-args args (:inputs ugen-template))
        arg-kv-pairs (partition 2 parsed-args)
        arg-vals (map second arg-kv-pairs)
        buses (first arg-vals)
        channels-array (second arg-vals)]
    (if (or (sequential? buses) (sequential? channels-array))
      (let [buses (if (sequential? buses) buses [buses])
            channels-array (if (sequential? channels-array) channels-array [channels-array])]
            (for [bus buses
                  channel-idx (range (count channels-array))]
              (abstract-output-ugen ugen-template (+ bus channel-idx) (nth channels-array channel-idx))))
        (let [id (next-id)
              ugen-template (assoc ugen-template
                                   :inputs parsed-args
                                   :id id
                                   ;TODO calculation-rate stuff shouldn't be hardcoded
                                   :calculation-rate 2
                                   :outputs []
                                   :num-outputs 0)]
          {:id id
           :calculation-rate 2
           :ugens (apply merge {id ugen-template} (map :ugens args))
           :dependencies (apply union
                                (set (keep identity (map (fn [arg]
                                                           (when-let [arg-id (get arg :id)]
                                                             [id arg-id])) args)))
                                (map :dependencies args))}))))

(defn ensure-constant
  "If the constant is not in constants, conj'es it in. Returns a vector with the
  constants vector and the index of the constant"
  [constants constant]
  (let [index-of-constant (.indexOf constants constant)]
    (case index-of-constant
      -1 [(conj constants constant) (count constants)]
      [constants index-of-constant])))

(defn compile-inputs
  [ugen ugens constants]
  (def ugen ugen)
  (def ugens ugens)
  (def constants constants)
  (let [partitioned-inputs (partition 2 (:inputs ugen))
        input-vals (map second partitioned-inputs)]
    (loop [inputs-to-read input-vals
           inputs []
           consts constants]
      (if-let [input (first inputs-to-read)]
        (if (map? input)
          ;;ugen
          (let [ancestor (some (fn [m] (when (= (:id m) (ffirst input)) m)) ugens)]
            (recur (rest inputs-to-read) (conj inputs {:index-of-ugen (.indexOf ugens ancestor)
                                                       :index-of-output (second (first input))}) consts))
          ;;constant
          (let [[constants-vec constant-idx] (ensure-constant consts input)]
            (recur (rest inputs-to-read) (conj inputs {:index-of-ugen -1 :index-of-output constant-idx}) constants-vec)))
        [inputs consts]))))

(defn set-additional-ugen-fields
  "Sets num-inputs and special-index (if not already set)"
  [ugen]
  (-> (if (:special-index ugen) ugen (assoc ugen :special-index 0))
      (assoc :num-inputs (count (:inputs ugen)))))

(defn compile-ugens
  [ugen-graph]
  (let [dep-graph (reduce (fn [g [n1 n2]] (dep/depend g n1 n2)) (dep/graph) (:dependencies ugen-graph))
        ordered-ugen-ids (dep/topo-sort dep-graph)]
    (loop [ugen-ids ordered-ugen-ids
           ugens     []
           constants []]
      (if-let [ugen-id (first ugen-ids)]
        (let [ugen (get-in ugen-graph [:ugens (first ugen-ids)])
              [inputs consts] (compile-inputs ugen ugens constants)]
          (recur (rest ugen-ids) (conj ugens (merge
                                              (select-keys ugen [:ugen-name :calculation-rate
                                                                 :id
                                                                 :num-outputs :outputs :special-index])
                                              {:inputs inputs})) consts))
        [(map set-additional-ugen-fields ugens) constants]))))

(defn compile-synth
  [synthdef-name ugen-list]
  (let [ugen-graph (reduce (fn [acc node]
                             {:ugens (merge (:ugens acc) (:ugens node))
                              :dependencies (union (:dependencies acc) (:dependencies node))})
                           {}
                           ugen-list)
        [ugens constants] (compile-ugens ugen-graph)]
    {:file-type-id synthdef/SCgf
     :file-version 2
     :num-synthdefs 1 ;;not supporting multiple synthdef definitions
     :synthdefs [{:ugens ugens
                  :synthdef-name synthdef-name
                  :num-ugens (count ugens)
                  :num-param-names 0 ;;TODO
                  :num-params 0 ;;TODO
                  :param-names [] ;;TODO
                  :init-param-values [] ;;TODO
                  :constants constants
                  :num-constants (count constants)
                  :num-variants 0 ;;not supporting variants yet; don't know any use cases
                  :variants []}]}))

(compile-synth "pan-synth" (pan2 (sin-osc)))

(compile-synth "sin-out" (out 0 (sin-osc)))

(comment
  (scsynth/define-synth (synthdef/write-synthdef-buffer (compile-synth "sin-out" (out 0 (sin-osc 336)))))
  )


;; (reduce (fn [acc [n1 n2]] (conj acc n1 n2)) [] (:dependencies (compile-synth (pan2 (sin-osc)))))
;; topo-sort dependencies
;; order ugens by sort result

;;needs to spit out :id and out


(def sin-osc-data {:ugen-name   "SinOsc"
                   :rates       [:ar :kr]
                   :calculation-rate 2
                   :inputs      [:freq 440 :phase 0]
                   :num-outputs 1})

(def pan2-data {:ugen-name   "Pan"
                :rates       [:ar :kr]
                :calculation-rate 2
                :inputs      [:in nil :pos 0 :level 1]
                :num-outputs 2})

(def out-data {:ugen-name "Out"
               :rates [:ar :kr]
               :calculation-rate 2
               :inputs [:bus 0 :channels-array nil]
               :num-outputs 0})

(def sin-osc (partial abstract-ugen sin-osc-data))
(def pan2 (partial abstract-ugen pan2-data))
(def out (partial abstract-output-ugen out-data))

(comment
  ;;these should all work
  (sin-osc 440 0) ;first arg 440 second arg 0
  (out 0 (sin-osc))

  (pan2 (sin-osc))

  (sin-osc 40 :phase 0.3)
  (sin-osc :freq 40 :phase 0.3)

  (pan2)

  (sin-osc [220 330 440])

  (sin-osc :freq [(sin-osc) 330 440])

  (sin-osc 440)   ;first arg 440 second arg default
  (sin-osc :freq 440 :phase 0) ;first arg 440 second arg 0

  (sin-osc :phase 0 :freq 440) ;first arg 440 second arg 0
  (sin-osc :phase 0.5) ;first arg default second arg 0.5
  (sin-osc 220 :phase 0.2);first arg 220 second arg 

  ;;args can be a vector with just ordered values (values can be ugens)
  ;;[220 0.2]
  ;;if a named value is given (keyword encountered), rest args must be key value pairs

  ;;TODO throw error on nil arg

  (fill-in-ordered-args [0 1 ] [:freq 220 :phase 0])
  (fill-in-ordered-args [0 1 ] [:freq 220 :phase 0])

  (parse-args [] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])
  (parse-args [1 2 :freq 330 :foo 1200] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])
  (parse-args [1 2 :freq 330 ] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])

  (parse-args [330] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])
  (parse-args [(sin-osc)] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])
  (parse-args [330 0.5] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])
  (parse-args [] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])

  (partition 2 [:freq 220 :phase 0.1 :mul 0.3 :add 0.5])


  (map #(if (map? %)
          (select-keys % [:id])
          %) (parse-args [(first (sin-osc))] [:freq 220 :phase 0.1 :mul 0.3 :add 0.5]))

  (map? :f)
  (map? )
  

  (first-keyword-index [0 1 2 :3])
  (drop 3 [0 1 2 :3])
  (take 3 [0 1 2 :3])
  (first-keyword-index [0 1 2 3])
  (first-keyword-index [0 :1 2 3])
  (drop 1 [0 :1 2 3])

  (pan2 (sin-osc))

  (reduce (fn [acc node]
            {:ugens (merge (:ugens acc) (:ugens node))
             :dependencies (union (:dependencies acc) (:dependencies node))})
          {}
          (pan2 (sin-osc [220 330])))

  (pan2 (sin-osc [220 330]))
  (reduce (fn [acc node]
            {:ugens (merge (:ugens acc) (:ugens node))
             :dependencies (union (:dependencies acc) (:dependencies node))})
          {}
          (pan2))

  (pan2)
  
  (sin-osc (sin-osc))


  ;;do I wanna use an output proxy concept?
  ;;annoyed because expansion happens with num-outputs, but also with num-inputs

  ;;(sin [220 330 440])
  (sin-osc [220 330 440])
  (sin-osc (sin-osc [220 330 440]))

  ;;(sin 220) (sin 330) (sin 440)
)

