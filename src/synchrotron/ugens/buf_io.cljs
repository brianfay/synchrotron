(ns synchrotron.ugens.buf-io
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

(def buf-rd-data {:ugen-name "BufRd"
                  :rates [:ar :kr]
                  :inputs [:num-channels 1 :buf-num 0 :phase 0 :loop 0 :interpolation 2]
                  :num-outputs :variadic})

(def buf-rd    (partial abstract-ugen buf-rd-data))
(def buf-rd:ar (partial abstract-ugen (assoc buf-rd-data :calculation-rate :ar)))
(def buf-rd:kr (partial abstract-ugen (assoc buf-rd-data :calculation-rate :kr)))

(def buf-wr-data {:ugen-name "BufWr"
                  :rates [:ar :kr]
                  :inputs [:input-array nil :buf-num 0 :phase 0 :loop 1]
                  :num-outputs 1})

(def buf-wr    (partial abstract-ugen buf-wr-data))
(def buf-wr:ar (partial abstract-ugen (assoc buf-wr-data :calculation-rate :ar)))
(def buf-wr:kr (partial abstract-ugen (assoc buf-wr-data :calculation-rate :kr)))
