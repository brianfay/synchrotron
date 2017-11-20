(ns synchrotron.core
  (:require [synchrotron.scsynth :as scsynth]
            [synchrotron.synthdef :as synthdef]))

(enable-console-print!)

(defn -main [& _]
  (scsynth/start-scsynth))

(set! *main-cli-fn* -main)
