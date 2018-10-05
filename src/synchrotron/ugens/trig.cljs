(ns synchrotron.ugens.trig
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def phasor-data {:ugen-name   "Phasor"
                  :rates       [:ar :kr]
                  :inputs      [:trig 0 :rate 1 :start 0 :end 1 :reset-pos 0]
                  :num-outputs 1})

(def phasor (partial abstract-ugen phasor-data))
(def phasor:ar (partial abstract-ugen phasor-data :calculation-rate :ar))
(def phasor:kr (partial abstract-ugen phasor-data :calculation-rate :kr))
