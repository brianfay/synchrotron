(ns synchrotron.ugens
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Binary Ops
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Buffer IO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;num-channels is actually a concept of "MultiOutUgen" and so this doesn't work
(def buf-rd-data {:ugen-name "BufRd"
                  :rates [:ar :kr]
                  :inputs [:num-channels 1 :buf-num 0 :phase 0 :loop 0 :interpolation 2]
                  ;;sclang has a variable called argsNamesInputsOffsets, which you can set to 2 to NOT send the first input to scsynth.
                  ;;it's very confusing.
                  :treat-num-channels-as-input false
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delays
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def delay-n-data {:ugen-name "DelayN"
                   :rates  [:ar :kr]
                   :inputs [:in 0 :max-delay-time 0.2 :delay-time 0.2]
                   :num-outputs 1})

(def delay-n (partial abstract-ugen delay-n-data))
(def delay-n:ar (partial abstract-ugen (assoc delay-n-data :calculation-rate :ar)))
(def delay-n:kr (partial abstract-ugen (assoc delay-n-data :calculation-rate :kr)))

(def comb-n-data {:ugen-name "CombN"
                  :rates [:ar :kr]
                  :inputs [:in 0 :max-delay-time 0.2 :delay-time 0.2 :decay-time 1]
                  :num-outputs 1})

(def comb-n (partial abstract-ugen comb-n-data))
(def comb-n:ar (partial abstract-ugen (assoc comb-n-data :calculation-rate :ar)))
(def comb-n:kr (partial abstract-ugen (assoc comb-n-data :calculation-rate :kr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; IO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def out-data {:ugen-name "Out"
               :rates [:ar :kr]
               :inputs [:bus 0 :channels-array nil]
               :num-outputs 0})

(def out    (partial abstract-ugen out-data))
(def out:ar (partial abstract-ugen (assoc out-data :calculation-rate :ar)))
(def out:kr (partial abstract-ugen (assoc out-data :calculation-rate :kr)))

(def replace-out-data {:ugen-name "ReplaceOut"
                       :rates [:ar :kr]
                       :inputs [:bus 0 :channels-array nil]
                       :num-outputs 0})

(def replace-out    (partial abstract-ugen replace-out-data))
(def replace-out:ar (partial abstract-ugen (assoc replace-out-data :calculation-rate :ar)))
(def replace-out:kr (partial abstract-ugen (assoc replace-out-data :calculation-rate :kr)))

(def in-data {:ugen-name "In"
              :rates [:ar :kr]
              :inputs [:bus 0 :num-channels 1]
              :num-outputs :variadic})

(def in    (partial abstract-ugen in-data))
(def in:ar (partial abstract-ugen (assoc in-data :calculation-rate :ar)))
(def in:kr (partial abstract-ugen (assoc in-data :calcutaion-rate :kr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Info
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def info-ugen-base {:rates [:ir]
                     :inputs nil
                     :num-outputs 1})

(def sample-rate-data (assoc info-ugen-base :ugen-name "SampleRate"))
(def sample-rate    (partial abstract-ugen sample-rate-data))
(def sample-rate:ir (partial abstract-ugen (assoc sample-rate-data :calculation-rate :ir)))

(def sample-dur-data (assoc info-ugen-base :ugen-name "SampleDur"))
(def sample-dur    (partial abstract-ugen sample-dur-data))
(def sample-dur:ir (partial abstract-ugen (assoc sample-dur-data :calculation-rate :ir)))

(def radians-per-sample-data (assoc info-ugen-base :ugen-name "RadiansPerSample"))
(def radians-per-sample    (partial abstract-ugen radians-per-sample-data))
(def radians-per-sample:ir (partial abstract-ugen (assoc radians-per-sample-data :calculation-rate :ir)))

(def block-size-data (assoc info-ugen-base :ugen-name "BlockSize"))
(def block-size    (partial abstract-ugen block-size-data))
(def block-size:ir (partial abstract-ugen (assoc block-size-data :calculation-rate :ir)))

(def control-rate-data (assoc info-ugen-base :ugen-name "ControlRate"))
(def control-rate    (partial abstract-ugen control-rate-data))
(def control-rate:ir (partial abstract-ugen (assoc control-rate-data :calculation-rate :ir)))

(def control-dur-data (assoc info-ugen-base :ugen-name "ControlDur"))
(def control-dur    (partial abstract-ugen control-dur-data))
(def control-dur:ir (partial abstract-ugen (assoc control-dur-data :calculation-rate :ir)))

(def subsample-offset-data (assoc info-ugen-base :ugen-name "SubsampleOffset"))
(def subsample-offset    (partial abstract-ugen subsample-offset-data))
(def subsample-offset:ir (partial abstract-ugen (assoc subsample-offset-data :calculation-rate :ir)))

(def num-output-buses-data (assoc info-ugen-base :ugen-name "NumOutputBuses"))
(def num-output-buses (partial abstract-ugen num-output-buses-data))
(def num-output-buses:ir (partial abstract-ugen (assoc num-output-buses-data :calculation-rate :ir)))

(def num-input-buses-data (assoc info-ugen-base :ugen-name "NumInputBuses"))
(def num-input-buses (partial abstract-ugen num-input-buses-data))
(def num-input-buses:ir (partial abstract-ugen (assoc num-input-buses-data :calculation-rate :ir)))

(def num-audio-buses-data (assoc info-ugen-base :ugen-name "NumAudioBuses"))
(def num-audio-buses (partial abstract-ugen num-audio-buses-data))
(def num-audio-buses:ir (partial abstract-ugen (assoc num-audio-buses-data :calculation-rate :ir)))

(def num-control-buses-data (assoc info-ugen-base :ugen-name "NumControlBuses"))
(def num-control-buses (partial abstract-ugen num-control-buses-data))
(def num-control-buses:ir (partial abstract-ugen (assoc num-control-buses-data :calculation-rate :ir)))

(def num-buffers-data (assoc info-ugen-base :ugen-name "NumBuffers"))
(def num-buffers (partial abstract-ugen num-buffers-data))
(def num-buffers:ir (partial abstract-ugen (assoc num-buffers-data :calculation-rate :ir)))

(def node-id-data (assoc info-ugen-base :ugen-name "NodeID"))
(def node-id (partial abstract-ugen node-id-data))
(def node-id:ir (partial abstract-ugen (assoc node-id-data :calculation-rate :ir)))

(def num-running-synths-data (assoc info-ugen-base :ugen-name "NodeID" :inputs [:kr :ir]))
(def num-running-synths (partial abstract-ugen num-running-synths-data))
(def num-running-synths:kr (partial abstract-ugen num-running-synths-data :calculation-rate :kr))
(def num-running-synths:ir (partial abstract-ugen num-running-synths-data :calculation-rate :ir))

(def buf-info-ugen-base {:rates [:kr :ir]
                         :inputs [:bufnum 0]
                         :num-outputs 1})

(def buf-sample-rate-data (assoc buf-info-ugen-base :ugen-name "BufSampleRate"))
(def buf-sample-rate    (partial abstract-ugen buf-sample-rate-data))
(def buf-sample-rate:kr (partial abstract-ugen (assoc buf-sample-rate-data :calculation-rate :kr)))
(def buf-sample-rate:ir (partial abstract-ugen (assoc buf-sample-rate-data :calculation-rate :ir)))

(def buf-rate-scale-data (assoc buf-info-ugen-base :ugen-name "BufRateScale"))
(def buf-rate-scale    (partial abstract-ugen buf-rate-scale-data))
(def buf-rate-scale:kr (partial abstract-ugen (assoc buf-rate-scale-data :calculation-rate :kr)))
(def buf-rate-scale:ir (partial abstract-ugen (assoc buf-rate-scale-data :calculation-rate :ir)))

(def buf-frames-data (assoc buf-info-ugen-base :ugen-name "BufFrames"))
(def buf-frames    (partial abstract-ugen buf-frames-data))
(def buf-frames:kr (partial abstract-ugen (assoc buf-frames-data :calculation-rate :kr)))
(def buf-frames:ir (partial abstract-ugen (assoc buf-frames-data :calculation-rate :ir)))

(def buf-samples-data (assoc buf-info-ugen-base :ugen-name "BufSamples"))
(def buf-samples    (partial abstract-ugen buf-samples-data))
(def buf-samples:kr (partial abstract-ugen (assoc buf-samples-data :calculation-rate :kr)))
(def buf-samples:ir (partial abstract-ugen (assoc buf-samples-data :calculation-rate :ir)))

(def buf-dur-data (assoc buf-info-ugen-base :ugen-name "BufDur"))
(def buf-dur    (partial abstract-ugen buf-dur-data))
(def buf-dur:kr (partial abstract-ugen (assoc buf-dur-data :calculation-rate :kr)))
(def buf-dur:ir (partial abstract-ugen (assoc buf-dur-data :calculation-rate :ir)))

(def buf-channels-data (assoc buf-info-ugen-base :ugen-name "BufChannels"))
(def buf-channels    (partial abstract-ugen buf-channels-data))
(def buf-channels:kr (partial abstract-ugen (assoc buf-channels-data :calculation-rate :kr)))
(def buf-channels:ir (partial abstract-ugen (assoc buf-channels-data :calculation-rate :ir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Line
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def line-data {:ugen-name "Line"
                :rates [:ar :kr]
                :inputs [:start 0.0 :end 1.0 :dur 1.0 :done-action 0]
                :num-outputs 1})

(def line    (partial abstract-ugen line-data))
(def line:ar (partial abstract-ugen (assoc line-data :calculation-rate :ar)))
(def line:kr (partial abstract-ugen (assoc line-data :calculation-rate :kr)))

(def x-line-data {:ugen-name "XLine"
                  :rates [:ar :kr]
                  :inputs [:start 1.0 :end 2.0 :dur 1.0 :done-action 0]
                  :num-outputs 1})

(def x-line    (partial abstract-ugen x-line-data))
(def x-line:ar (partial abstract-ugen (assoc x-line-data :calculation-rate :ar)))
(def x-line:kr (partial abstract-ugen (assoc x-line-data :calculation-rate :kr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Noise
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Osc
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def sin-osc-data {:ugen-name   "SinOsc"
                   :rates       [:ar :kr]
                   :inputs      [:freq 440 :phase 0]
                   :num-outputs 1})

(def sin-osc    (partial abstract-ugen sin-osc-data))
(def sin-osc:ar (partial abstract-ugen (assoc sin-osc-data :calculation-rate :ar)))
(def sin-osc:kr (partial abstract-ugen (assoc sin-osc-data :calculation-rate :kr)))

(def impulse-data {:ugen-name "Impulse"
                   :rates [:ar :kr]
                   :inputs [:freq 440 :phase 0]
                   :num-outputs 1})

(def impulse (partial abstract-ugen impulse-data))
(def impulse:ar (partial abstract-ugen (assoc impulse-data :calculation-rate :ar)))
(def impulse:kr (partial abstract-ugen (assoc impulse-data :calculation-rate :kr)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pan
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def pan2-data {:ugen-name "Pan2"
                :rates [:ar :kr]
                :inputs [:in nil :pos 0 :level 1]
                :num-outputs 2})

(def pan2    (partial abstract-ugen pan2-data))
(def pan2:ar (partial abstract-ugen (assoc pan2-data :calculation-rate :ar)))
(def pan2:kr (partial abstract-ugen (assoc pan2-data :calculation-rate :kr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Trig
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def phasor-data {:ugen-name   "Phasor"
                  :rates       [:ar :kr]
                  :inputs      [:trig 0 :rate 1 :start 0 :end 1 :reset-pos 0]
                  :num-outputs 1})

(def phasor (partial abstract-ugen phasor-data))
(def phasor:ar (partial abstract-ugen (assoc phasor-data :calculation-rate :ar)))
(def phasor:kr (partial abstract-ugen (assoc phasor-data :calculation-rate :kr)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Envelope
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def linen-data {:ugen-name "Linen"
                 :rates [:kr]
                 :inputs [:gate 1 :attack-time 0.01 :sus-level 1 :release-time 1.0 :done-action 0]
                 :num-outputs 1})

(def linen (partial abstract-ugen linen-data))
(def linen:kr (partial abstract-ugen (assoc linen-data :calculation-rate :kr)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bela
;; WARNING -- these will only work on the Bela fork of SuperCollider
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def multiplex-analog-in-data {:ugen-name "MultiplexAnalogIn"
                               :rates [:ar :kr]
                               :inputs [:analog-pin 0 :mux-channel 0]
                               :num-outputs 1})

(def multiplex-analog-in (partial abstract-ugen multiplex-analog-in-data))
(def multiplex-analog-in:ar (partial abstract-ugen (assoc multiplex-analog-in-data :calculation-rate :ar)))
(def multiplex-analog-in:kr (partial abstract-ugen (assoc multiplex-analog-in-data :calculation-rate :kr)))

(def analog-in-data {:ugen-name "AnalogIn"
                     :rates [:ar :kr]
                     :inputs [:analog-pin 0]
                     :num-outputs 1})

(def analog-in (partial abstract-ugen analog-in-data))
(def analog-in:ar (partial abstract-ugen (assoc analog-in-data :calculation-rate :ar)))
(def analog-in:kr (partial abstract-ugen (assoc analog-in-data :calculation-rate :kr)))

(def analog-out-data {:ugen-name "AnalogOut"
                     :rates [:ar :kr]
                     :inputs [:analog-pin 0]
                     :num-outputs 0});;TODO test - reference implementation no-ops writeOutputSpecs, probably need something like that

(def analog-out (partial abstract-ugen analog-out-data))
(def analog-out:ar (partial abstract-ugen (assoc analog-out-data :calculation-rate :ar)))
(def analog-out:kr (partial abstract-ugen (assoc analog-out-data :calculation-rate :kr)))

(def digital-in-data {:ugen-name "DigitalIn"
                      :rates [:ar :kr]
                      :inputs [:digital-pin 0]
                      :num-outputs 1})

(def digital-in (partial abstract-ugen digital-in-data))
(def digital-in:ar (partial abstract-ugen (assoc digital-in-data :calculation-rate :ar)))
(def digital-in:kr (partial abstract-ugen (assoc digital-in-data :calculation-rate :kr)))

(def digital-out-data {:ugen-name "DigitalOut"
                       :rates [:ar :kr]
                       :inputs [:digital-pin 0]
                       :num-outputs 0});;TODO test - reference implementation no-ops writeOutputSpecs, probably need something like that

(def digital-out (partial abstract-ugen digital-out-data))
(def digital-out:ar (partial abstract-ugen (assoc digital-out-data :calculation-rate :ar)))
(def digital-out:kr (partial abstract-ugen (assoc digital-out-data :calculation-rate :kr)))

(def digital-io-data {:ugen-name "DigitalIO"
                      :rates [:ar :kr]
                      :inputs [:digital-pin 0 :output 0 :pin-mode 0]
                      :num-outputs 1})

(def digital-io (partial abstract-ugen digital-io-data))
(def digital-io:ar (partial abstract-ugen (assoc digital-io-data :calculation-rate :ar)))
(def digital-io:kr (partial abstract-ugen (assoc digital-io-data :calculation-rate :ar)))
