(ns synchrotron.ugens.noise
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def lf-noise-data {:rates [:ar :kr]
                    :inputs [:freq 500]
                    :num-outputs 1})

(def lf-noise-0    (partial abstract-ugen (assoc lf-noise-data :ugen-name "LFNoise0")))

(def lf-noise-0:ar (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise0"
                                                 :calculation-rate :ar)))

(def lf-noise-0:kr (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise0"
                                                 :calculation-rate :kr)))

(def lf-noise-1    (partial abstract-ugen (assoc lf-noise-data :ugen-name "LFNoise1")))

(def lf-noise-1:ar (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise1"
                                                 :calculation-rate :ar)))

(def lf-noise-1:kr (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise1"
                                                 :calculation-rate :kr)))

(def lf-noise-2    (partial abstract-ugen (assoc lf-noise-data :ugen-name "LFNoise2")))

(def lf-noise-2:ar (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise2"
                                                 :calculation-rate :ar)))
 
(def lf-noise-2:kr (partial abstract-ugen (assoc lf-noise-data
                                                 :ugen-name "LFNoise2"
                                                 :calculation-rate :kr)))
