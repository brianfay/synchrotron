(ns synchrotron.core
  (:require [synchrotron.scsynth :as scsynth]
            [synchrotron.synthdef :as synthdef]))

(enable-console-print!)

;;will probably want to lose this eventually and use this project as a library
(defn -main [& _]
  (scsynth/start-scsynth))

(set! *main-cli-fn* -main)
