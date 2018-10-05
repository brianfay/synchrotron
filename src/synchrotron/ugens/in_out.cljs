(ns synchrotron.ugens.in-out
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def out-data {:ugen-name "Out"
               :rates [:ar :kr]
               :inputs [:bus 0 :channels-array nil]
               :num-outputs 0})

(def out    (partial abstract-ugen out-data))
(def out:ar (partial abstract-ugen (assoc out-data :calculation-rate :ar)))
(def out:kr (partial abstract-ugen (assoc out-data :calculation-rate :kr)))

(def replace-out-data {:ugen-name "ReplaceOut"
                       :rates [:ar :kr]
                       :inputs [:bus 0 :channels-array nil]
                       :num-outputs 0})

(def replace-out (partial abstract-ugen replace-out-data))
(def out:ar      (partial abstract-ugen (assoc replace-out-data :calculation-rate :ar)))
(def out:kr      (partial abstract-ugen (assoc replace-out-data :calculation-rate :kr)))

(def in-data {:ugen-name "In"
              :rates [:ar :kr]
              :inputs [:bus 0 :num-channels 1]
              :num-outputs :variadic})

(def in    (partial abstract-ugen in-data))
(def in:ar (partial abstract-ugen (assoc in-data :calculation-rate :ar)))
(def in:kr (partial abstract-ugen (assoc in-data :calcutaion-rate :kr)))
