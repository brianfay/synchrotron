(ns scratchpad
  (:require [synchrotron.scsynth :as scsynth]
            [synchrotron.ugens :as u])
  (:require-macros [synchrotron.macros :as macros :refer [defsynth]]))

(defonce performance (aget (js/require "perf_hooks") "performance"))

(comment
  ;;boot supercollider (or reboot, if it segfaulted or something)
  (do (scsynth/kill-scsynth)
      (scsynth/start-scsynth))

  ;;add a very simple synth definition (also compiles it and sends to the definition to scsynth)
  (defsynth simple-sine []
    (u/out [0 1] (u/mul 0.03 (u/sin-osc 440))))

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
      (u/replace-out [0 1] [(u/add in-l (u/comb-n in-l 2 delay-l-time 1))
                            (u/add in-r (u/comb-n in-r 2 delay-r-time 1))])))

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


  ;;load some example sound files (from a scene in Breaking Bad)
  (scsynth/alloc-buffer-from-soundfile 1 "samples/synchrotrons-stereo.wav")
  (scsynth/alloc-buffer-from-soundfile 1 "samples/patterns-stereo.wav")

  (defsynth loop-walt [buf-num 1 rate 1 out-bus 0]
    (let [;;wobble (u/mul:kr 0 (u/sin-osc:kr (u/lf-noise-1:kr 0.2)))
          ;; rate   (u/add:kr rate wobble)
          phasor (u/phasor:ar 0 (u/mul:kr rate (u/buf-rate-scale:kr buf-num)) 0 (u/buf-frames:kr buf-num))
          buf-rd (u/buf-rd:ar :num-channels 2
                              :buf-num buf-num
                              :phase phasor
                              :loop 1
                              :interpolation 2)]
      (u/out:ar out-bus buf-rd)))

  (do
    (scsynth/deep-free 0)
    (scsynth/add-synth-to-head loop-walt 1 0)
    (scsynth/add-synth-to-head loop-walt 2 0 [:out-bus 1 :rate 1.01]))

  ;;WARNING: for these to work, scsynth must have been started with flags setting number of various channels, like this:
  ;;scsynth -u 57110 -z 16 -J 8 -K 8 -G 16
  (defsynth bela-test [in-pin 7]
    (u/out [0 1] (u/mul:ar
                  0.03
                  (u/mul:ar (u/digital-io:ar in-pin) (u/sin-osc:ar 330)))))

  (do
    (scsynth/deep-free 0)
    (scsynth/add-synth-to-head bela-test 1 0 [:in-pin 6])
    (scsynth/add-synth-to-head bela-test 2 0 [:in-pin 7])
    (scsynth/add-synth-to-head bela-test 3 0 [:in-pin 10])
    )

  (defsynth send-trig []
    (u/send-trig:kr (u/impulse:kr 2) 0 0))

  (do
    (scsynth/deep-free 0)
    (scsynth/add-synth-to-head send-trig 1 0)
    )

  (defsynth audio-input [in-bus 0 out-bus 0]
    (u/out:ar out-bus (u/in:ar in-bus)))

  (defsynth record-loop [buf-num 0 in-bus 0 trig 1 run 1]
    (let [in (u/in:ar in-bus)]
      (u/record-buf:ar :input-array in
                       :buf-num buf-num
                       :offset 0
                       :rec-level 1
                       :pre-level 0
                       :run run
                       :loop 1
                       :trigger trig)))

  (defsynth play-loop [buf-num 0 out-bus 0 rate 1 trig 1 loop-end 1]
    (let [ph (u/phasor:ar :trig trig
                          :rate (u/mul:kr
                                 (u/buf-rate-scale:kr buf-num)
                                 rate)
                          :end (u/mul:kr (u/sample-rate:ir) loop-end))
          out (u/buf-rd:ar 1 buf-num ph :loop 1 :interpolation 2)]
      (u/out:ar out-bus out)))

  ;;add a weird backwards delay
  (do
    (scsynth/deep-free 0)
    (scsynth/alloc-buffer 3 (* 48000 2) 1)
    (scsynth/add-synth-to-head record-loop 1 0 [:buf-num 3 :in-bus 8])
    (scsynth/add-synth-after play-loop 2 1 [:buf-num 3 :rate -2 :out-bus 0 :loop-end 1])
    (scsynth/add-synth-after play-loop 3 1 [:buf-num 3 :rate -2 :out-bus 1 :loop-end 1]))

  (do
    (scsynth/deep-free 0)
    (scsynth/alloc-buffer 1 (* 48000 100) 1)
    ;;8 is the first "hardware" input bus, using jack you can reroute audio from other applications to this
    ;;(I'm using Patchage to route my PulseAudio JACK Sink outputs into SuperCollider)
    (scsynth/add-synth-to-head audio-input 1 0 [:in-bus 8 :out-bus 0])

    ;;add the record-head, which won't record until activated
    (scsynth/add-synth-after record-loop 2 1 [:buf-num 1 :in-bus 0 :trig -1 :run 0])

    ;;add the play-head
    (scsynth/add-synth-after play-loop 3 2 [:buf-num 1 :rate 1 :out-bus 1 :loop-end 2]))

  (def timestamp (atom nil))
  ;;start recording
  (do
    (reset! timestamp (.now performance))
    (scsynth/set-control 2 :trig 1 :run 1)
    (scsynth/run-node 3 0))


  ;;stop recording and play loop
  (let [loop-time (/ (- (.now performance) @timestamp) 1000)]
    (scsynth/set-control 2 :trig -1 :run 0)
    (scsynth/run-node 3 1)
    (scsynth/set-control 3 :trig 1 :loop-end loop-time))

  ;;vapourwave recipe:
  ;;loop a few bars of 80s japanese jazz fusion
  ;;slow it down, play it backwards, add some delay
  (scsynth/set-control 3 :rate -0.68)
  (scsynth/add-synth-after delay 4 3)
  (scsynth/set-control 4 :delay-r-time 0.4)
)
