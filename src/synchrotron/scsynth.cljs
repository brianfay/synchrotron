(ns synchrotron.scsynth
  (:require [cljs.nodejs :as nodejs]
            [dgram]
            [child_process :as child-process]
            [synchrotron.common-util :refer [green red]]
            [goog.object]))

(defonce osc-min (js/require "osc-min"));;strangely, requiring this in the ns block doesn't work until you eval it a bunch of times
(defonce scsynth-process (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; process
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- mk-process-env []
  (let [obj (.create js/Object (.-env nodejs/process))]
    (goog.object/set obj "SC_JACK_DEFAULT_INPUTS" "system")
    (goog.object/set obj "SC_JACK_DEFAULT_OUTPUTS" "system")
    #js{:env obj}))

(defn start-scsynth
  "Start an scsynth process if we have not already started one
  (this may have issues if anyn other scsynth instances are running on the computer)"
  []
  (when-not @scsynth-process
    (let [env (.create js/Object (.-env nodejs/process))
          scsynth (child-process/spawn "scsynth" #js["-u" "57110"] (mk-process-env))]
      (println (green "Started scsynth"))
      (.on (.-stdout scsynth) "data" #(println (green %)))
      (.on (.-stderr scsynth) "data" #(println (red %)))
      (reset! scsynth-process scsynth))))

(defn kill-scsynth
  "Send program interrupt signal to child scsynth process (kill it)"
  []
  (when-let [scsynth @scsynth-process]
    (.kill scsynth "SIGINT")
    (reset! scsynth-process nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; I/O
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-osc-callback [msg]
  (let [osc-map (-> (.fromBuffer osc-min msg)
                    (js->clj :keywordize-keys true))]
    (apply str (interpose " " (cons (:address osc-map) (map :value (:args osc-map)))))))

(defn create-socket []
  (let [socket (dgram/createSocket "udp4")]
    (do
      (.on socket "listening" #(println "udp is listening"))
      (.on socket "message" (fn [msg info]
                              (println (parse-osc-callback msg))))
      (.on socket "error" #(println (str "socket error: " %)))
      (.on socket "close" #(println (str "closing udp socket " %)))
      socket)))

(defonce udp-socket (create-socket))

(defn array->osc [arr]
  (.toBuffer osc-min arr))

(defn call-scsynth [addr & args]
  "Sends an OSC buffer to scsynth over a udp socket, using the given address and arguments."
  (let [msg (array->osc (clj->js {:address addr :args (clj->js args)}))]
    (.send udp-socket msg 0 (.-length msg) 57110 "localhost"
           (fn [err bytes]
             (if (and err (not (= 0 err))) (println (str "There was an error: " err)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Synth Definition Commands:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn define-synth
  [buffer]
  (call-scsynth "d_recv" buffer))

(defn delete-synthdef
  [synthdef-name]
  (call-scsynth "d_free" synthdef-name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Node Commands:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn free-node
  [id]
  (call-scsynth "n_free" id))

(defn deep-free
  [id]
  (call-scsynth "g_deepFree" id))

(defn set-control
  [id name val]
  (call-scsynth "n_set" id name val))

(defn map-control-to-bus
  [id name bus]
  (call-scsynth "n_map" id name bus))

(defn move-node-before
  [id target]
  (call-scsynth "n_before" id target))

(defn move-node-after
  [id target]
  (call-scsynth "n_after" id target))

(defn order-nodes
  [add-action target node-ids]
  (apply call-scsynth "n_order" add-action target node-ids))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Synth Commands:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-synth
  ([synthdef id add-action target controls]
   (apply call-scsynth "s_new" synthdef id add-action target controls))
  ([synthdef id add-action target]
   (call-scsynth "s_new" synthdef id add-action target)))

(defn add-synth-to-head
  ([synthdef id target controls]
   (add-synth synthdef id 0 target controls))
  ([synthdef id target]
   (add-synth synthdef id 0 target)))

(defn add-synth-to-tail
  ([synthdef id target controls]
   (add-synth synthdef id 1 target controls))
  ([synthdef id target]
   (add-synth synthdef id 1 target)))

(defn add-synth-before
  ([synthdef id target controls]
   (add-synth synthdef id 2 target controls))
  ([synthdef id target]
   (add-synth synthdef id 2 target)))

(defn add-synth-after
  ([synthdef id target controls]
   (add-synth synthdef id 3 target controls))
  ([synthdef id target]
   (add-synth synthdef id 3 target)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Group Commands:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-group
  "Adds a group with the given id, add-action, and target id."
  [id add-action target]
  (call-scsynth "g_new" id add-action target))

(defn free-in-group
  [id]
  (call-scsynth "g_freeAll" id))

(defn alloc-buffer [buf-num num-frames channels]
  (call-scsynth "b_alloc" buf-num num-frames channels))

(defn alloc-buffer-from-soundfile
  ([buf-num sf-path]
   (alloc-buffer-from-soundfile buf-num sf-path 0 0)) ;; whole sound file is read if num-frames is <= 0
  ([buf-num sf-path starting-frame num-frames]
   (call-scsynth "b_allocRead" buf-num sf-path starting-frame num-frames)))

(defn free-buffer [num]
  (call-scsynth "b_free"))
