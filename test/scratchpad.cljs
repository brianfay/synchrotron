(ns synchrotron.scratchpad
  (:require [synchrotron.scsynth :as scsynth]
            [synchrotron.synthdef.compiler]
            [synchrotron.ugens.osc :as osc :refer [sin-osc sin-osc:ar sin-osc:kr]]
            [synchrotron.ugens.in-out :as in-out :refer [out]]
            [synchrotron.ugens.basic-ops :as basic-ops :refer [add mul]]
            [synchrotron.ugens.pan :as pan :refer [pan2]])
  (:require-macros [synchrotron.macros :as macros :refer [defsynth]]))

;; (scsynth/start-synth)

(defsynth sine []
  (let [amp-lfo (mul 0.1 (sin-osc 0.2))
        freq-lfo (mul 220 (sin-osc 1))]
    (out 0 (mul amp-lfo (sin-osc (add freq-lfo 440))))
    (out 1 (mul amp-lfo (sin-osc (add freq-lfo 440))))))

(defsynth test [a 1] (def a a) (sin-osc 220))

(defsynth sinus [freq 770 outt 3]
  (def freq freq)
  (def outt outt)
  (out outt (mul (sin-osc freq) 0.2)))

;; (out [0 1] (pan2 (sin-osc)));;could be explicit about synths with multiple outputs
;; (out 0 (pan2 (sin-osc)))    ;;not sure which way I'd rather express this

(defsynth foo []
  (let [max 8]
    (for [i (range 1 max)]
      (let [amp-lfo (mul 0.01 (sin-osc :freq 0.1 :phase (* (/ i max) Math/PI)))
            oscil   (mul amp-lfo (sin-osc (* i 110)))
            pan (pan2 oscil (sin-osc 0.3 (* (/ i max) Math/PI)))]
        (out 0 pan)))))

(defsynth testme []
  (out 0 (pan2 (sin-osc 330) (sin-osc 0.2) 0.2)))

(comment
  (scsynth/add-synth-to-head sine 2 0)
  (scsynth/add-synth-to-head sine2 2 0)
  (scsynth/add-synth-to-head sinus 2 0 ["freq" 660 "outt" 1])
  (scsynth/add-synth-to-head sinus 2 0)
  (scsynth/add-synth-to-head sinus 3 0 ["freq" 220 "outt" 0])
  (scsynth/add-synth-to-head "siney" 3 0)
  (scsynth/add-synth-to-head "sinboyz" 3 0)
  (scsynth/set-control 3 "freq" 330)
  (scsynth/set-control 3 "outt" 0)
  (scsynth/set-control 2 "outt" 0)
  (scsynth/set-control 3 "outt" 0)
  (scsynth/add-synth-to-head foo 2 0)
  (scsynth/add-synth-to-head foo 3 0)
  (scsynth/add-synth-to-head foo 4 0)
  (scsynth/add-synth-to-head foo 5 0)

  (scsynth/add-synth-to-head testme 2 0 )
  (scsynth/add-synth-to-head testme 3 0 )
  (scsynth/add-synth-to-head testme 4 0 )
  (scsynth/add-synth-to-head testme 5 0 )
  (scsynth/deep-free 0))
