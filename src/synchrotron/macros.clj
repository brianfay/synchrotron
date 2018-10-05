(ns synchrotron.macros)

;;I'm assuming that there's always only one Control UGen and it's always using kr rate.
;;but not 100% sure this is true
(defn control-ugen
  [num-outputs out-index [param-name value]]
  {:id 0
    :out-index out-index
    :ugen-name "Control"
    :param-name (name param-name)
    :calculation-rate 1
    :ugens {0 {:ugen-name "Control"
               :rates [:kr]
               :calculation-rate 1
               :inputs []
               :num-outputs num-outputs
               ;;this needs to go into a vector because it would try to evaluate otherwise:
               :outputs (into [] (repeat num-outputs {:calculation-rate 1}))
               :id 0}}
    :value value})

(defn destructure-controls
  [control-bindings]
  ;;TODO support key destructuring sugar; a hacky solution will probably suffice
  (let [controls-vec (destructure control-bindings)
        ks (take-nth 2 controls-vec)
        kvs (partition 2 controls-vec)
        num-outputs (count ks)]
    (into [] (interleave ks (map-indexed (partial control-ugen num-outputs) kvs)))))

(defmacro defsynth
  [synthdef-symbol controls & body]
  (let [synthdef-name (name synthdef-symbol)
        control-bindings (destructure-controls controls)]
    `(do
       (reset! synchrotron.ugen/current-id 1)
       (reset! synchrotron.ugen/ugens-in-current-synthdef [])
       (doseq [control-ugen# (map second (partition 2 ~control-bindings))]
         (swap! synchrotron.ugen/ugens-in-current-synthdef conj control-ugen#))
       (let ~control-bindings
         (do ~@body
             (-> (#'synchrotron.synthdef.compiler/compile-synthdef ~synthdef-name)
                 (synchrotron.synthdef/write-synthdef-buffer)
                 (synchrotron.scsynth/define-synth))
             (def ~synthdef-symbol ~synthdef-name))))))
