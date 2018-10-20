(ns scratchpad
  (:require [synchrotron.scsynth :as scsynth]
            [synchrotron.ugens :as u])
  (:require-macros [synchrotron.macros :as macros :refer [defsynth]]))

(comment
  ;;boot supercollider (or reboot, if it segfaulted or something)
  (do (scsynth/kill-scsynth)
      (scsynth/start-scsynth))

  ;;add a very simple synth definition (also compiles it and sends to the definition to scsynth)
  (defsynth simple-sine []
    (u/out [0 1] (u/mul 0.3 (u/sin-osc 440))))

  ;;add a new instance of simple-sine to head of group 0
  (do
    (scsynth/deep-free 0) ;;this will free all nodes in the node tree, and give you silence
    (scsynth/add-synth-to-head simple-sine 1 0))

  ;;add another simple synth definition, this time with a controllable frequency
  (defsynth controllable-sine [freq 440]
    (u/out [0 1] (u/mul 0.3 (u/sin-osc freq))))

  ;;add a new instance of controllable sine, start at freq 660 and descend down a few times
  (do
    (scsynth/deep-free 0)
    (scsynth/add-synth-to-head controllable-sine 1 0 [:freq 660])
    (js/setTimeout #(scsynth/set-control 1 :freq 550) 1000)
    (js/setTimeout #(scsynth/set-control 1 :freq 440) 2000)
    (js/setTimeout #(scsynth/set-control 1 :freq 330) 3000)
    (js/setTimeout #(scsynth/deep-free 0) 4000))


  ;;here's a sine wave going through a linear amplitude envelope, with a bunch of controllable parameters
  (defsynth linens-n-things [freq 330 out-bus 0 impulse-rate 0.3 attack-time 0.01 release-time 0.03]
    (let [osc (u/mul 0.1 (u/sin-osc freq))
          pulse (u/impulse:kr impulse-rate) ;;NB: impulse must be kr here, get weird behaviour if using audio rate
          env (u/linen pulse attack-time 1 release-time 0)]
      (u/out out-bus (u/mul osc env))))

  ;;a simple stereo feedback delay effect
  (defsynth delay [delay-l-time 0.2 delay-r-time 0.2]
    (let [in-l (u/in 0)
          in-r (u/in 1)]
      (u/replace-out [0 1] [(u/add in-l (u/comb-n in-l 0.5 delay-l-time 0.5))
                            (u/add in-r (u/comb-n in-r 0.5 delay-r-time 0.5))])))

  ;;little generative experiment
  (let [fundamental (rand-nth [100 160 220])
        harmonics (map #(* fundamental %) (range 1 9))
        rand-note #(rand-nth harmonics)
        rand-env-time #(rand-nth [(* (/ (rand 100) 100) 0.2) ;;a higher probability of shorter envelopes being selected
                                  (* (/ (rand 100) 100) 0.2)
                                  (* (/ (rand 100) 100) 0.1)
                                  (/ (rand 100) 100)])
        rand-args (fn []
                    [:out-bus (rand-nth [0 1])
                     :freq (rand-note)
                     :attack-time  (rand-env-time)
                     :impulse-rate (/ (rand 100) 100)
                     :release-time (rand-env-time)])]
    (scsynth/deep-free 0)
    (scsynth/add-synth-to-head linens-n-things 1 0 (rand-args))
    (scsynth/add-synth-to-head linens-n-things 2 0 (rand-args))
    (scsynth/add-synth-to-head linens-n-things 3 0 (rand-args))
    (scsynth/add-synth-to-head linens-n-things 4 0 (rand-args))
    (scsynth/add-synth-to-tail delay 5 0 [:delay-l-time (/ (rand-int 10) 10) :delay-r-time (/ (rand-int 10) 10)]))

  )
