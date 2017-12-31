(ns synchrotron.ugens.osc
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def sin-osc-data {:ugen-name   "SinOsc"
                   :rates       [:ar :kr]
                   :inputs      [:freq 440 :phase 0]
                   :num-outputs 1})

(def sin-osc    (partial abstract-ugen sin-osc-data))
(def sin-osc:ar (partial abstract-ugen (assoc sin-osc-data :calculation-rate :ar)))
(def sin-osc:kr (partial abstract-ugen (assoc sin-osc-data :calculation-rate :kr)))
