(ns synchrotron.ugens.info-ugens
  (:require [synchrotron.ugen :as ugen :refer [abstract-ugen]]))

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
