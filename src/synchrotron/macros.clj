(ns synchrotron.macros)

(defmacro defsynth
  [synthdef-symbol params & body]
  (let [synthdef-name (name synthdef-symbol)]
    `(do (reset! synchrotron.ugen/current-id 0)
         (reset! synchrotron.ugen/ugens-in-current-synthdef [])
         (doall ~@body)
         (-> (synchrotron.synthdef.compiler/compile-synthdef ~synthdef-name)
             (synchrotron.synthdef/write-synthdef-buffer)
             (synchrotron.scsynth/define-synth))
         (def ~synthdef-symbol ~synthdef-name))))
