(ns synchrotron.ugens.basic-ops
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def binary-op-ugen-data {:ugen-name "BinaryOpUGen"
                          :rates [:ar :kr :demand :scalar]
                          :inputs [:a nil :b nil]
                          :num-outputs 1})

;;TODO infer calculation-rate from ugen inputs (but I'm not sure which rate should get preference if two different rates are given)
(def add    (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 0)))
(def add:ar (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 0)))
(def add:kr (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :kr :special-index 0)))

(def sub (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 1)))
(def sub:ar (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 1)))
(def sub:kr (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :kr :special-index 1)))

(def mul (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 2)))
(def mul:ar (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 2)))
(def mul:kr (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :kr :special-index 2)))

(def div (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 4)));;float division
(def div:ar (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 4)));;float division
(def div:kr (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :kr :special-index 4)));;float division

(def eq (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 6)))
(def eq:ar (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :ar :special-index 6)))
(def eq:kr (partial abstract-ugen (assoc binary-op-ugen-data :calculation-rate :kr :special-index 6)))
