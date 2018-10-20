(ns synchrotron.core
  (:require [synchrotron.scsynth :as scsynth]
            ;;requiring these to force evaluation - the defsynth macro references them so they need to be required somewhere
            [synchrotron.synthdef.compiler]
            [synchrotron.ugen]))

(enable-console-print!)

;;will probably want to lose this eventually and use this project as a library
(defn -main [& _]
  (scsynth/start-scsynth))

(set! *main-cli-fn* -main)
