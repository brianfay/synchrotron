(ns synchrotron.ugens.in-out
  (:require [synchrotron.ugen :as ugen :refer [abstract-output-ugen]]))

(def out-data {:ugen-name "Out"
               :rates [:ar :kr]
               :inputs [:bus 0 :channels-array nil]
               :num-outputs 0})

(def out    (partial abstract-output-ugen out-data))
(def out:ar (partial abstract-output-ugen (assoc out-data :calculation-rate :ar)))
(def out:kr (partial abstract-output-ugen (assoc out-data :calculation-rate :kr)))
