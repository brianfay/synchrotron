(ns synchrotron.ugens.basic-ops
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def binary-op-ugen-data {:ugen-name "BinaryOpUGen"
                          :rates [:ar :kr :demand :scalar]
                          :inputs [:a nil :b nil]
                          :num-outputs 1})

(def add (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 0)))
(def sub (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 1)))
(def mul (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 2)))
(def div (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 4)));;float division
(def eq (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 6)))
