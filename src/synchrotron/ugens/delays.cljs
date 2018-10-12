(ns synchrotron.ugens.delays
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def delay-n-data {:ugen-name "DelayN"
                   :rates  [:ar :kr]
                   :inputs [:in 0 :max-delay-time 0.2 :delay-time 0.2]
                   :num-outputs 1})

(def delay-n (partial abstract-ugen delay-n-data))
(def delay-n:ar (partial abstract-ugen (assoc delay-n-data :calculation-rate :ar)))
(def delay-n:kr (partial abstract-ugen (assoc delay-n-data :calculation-rate :kr)))

(def comb-n-data {:ugen-name "CombN"
                  :rates [:ar :kr]
                  :inputs [:in 0 :max-delay-time 0.2 :delay-time 0.2 :decay-time 1]
                  :num-outputs 1})

(def comb-n (partial abstract-ugen comb-n-data))
(def comb-n:ar (partial abstract-ugen (assoc comb-n-data :calculation-rate :ar)))
(def comb-n:kr (partial abstract-ugen (assoc comb-n-data :calculation-rate :kr)))
