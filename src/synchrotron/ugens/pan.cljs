(ns synchrotron.ugens.pan
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def pan2-data {:ugen-name "Pan2"
                :rates [:ar :kr]
                :inputs [:in nil :pos 0 :level 1]
                :num-outputs 2})

(def pan2    (partial abstract-ugen pan2-data))
(def pan2:ar (partial abstract-ugen (assoc pan2-data :calculation-rate :ar)))
(def pan2:kr (partial abstract-ugen (assoc pan2-data :calculation-rate :kr)))
