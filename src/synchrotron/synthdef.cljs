(ns synchrotron.synthdef
  "Utils for reading a synthdef from binary format into a clojure map
  and writing from a clojure map into binary format"
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [synchrotron.data-util :refer [get-uint8 get-uint32 get-int32 get-uint16
                                           get-pstring get-float32-array-via
                                           data-view-reader]]
            [synchrotron.common-util :refer [green red niterate assocer]]))

;;This code is like an etude in functional programming.
;;But it's one of those tricky etudes that's difficult to sight-read, doesn't sound very good,
;;and doesn't really teach anything useful.

(defonce fs (nodejs/require "fs"))

(def SCgf 0x53436766);;magic 32-bit integer that supercollider uses to identity synthdef files

(defn read-file [path cb]
  (.readFile fs path cb))

(defn- assert-file-type-id
  [file-type-id]
  (when (not= file-type-id SCgf)
    (throw (js/Error "First 4 bytes of synthdef file should be SCgf"))))

(defn- parse-synthdef-header
  [dv-reader]
  (-> dv-reader
      (get-uint32 (assocer :file-type-id))
      (get-uint32 (assocer :file-version))
      (get-uint16 (assocer :num-synthdefs))))

(defn- parse-param-name
  [dv-reader]
  (-> dv-reader
      (get-pstring (assocer :temp-param-name))
      (get-uint32  (assocer :temp-param-index))
      (as-> $ (update-in $ [:parsed :temp :param-names]
                         conj [(get-in $ [:parsed :temp-param-name])
                               (get-in $ [:parsed :temp-param-index])]))
      (update :parsed dissoc :temp-param-name :temp-param-index)))

(defn- parse-param-names
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp :param-names] [])
      (as-> $ (niterate $ parse-param-name (get-in $ [:parsed :temp :num-param-names])))))

(defn- parse-input
  [dv-reader]
  (-> dv-reader
      (get-int32 (assocer :temp-input :index-of-ugen))
      (get-uint32 (assocer :temp-input :index-of-output))
      (as-> $ (update-in $ [:parsed :temp-ugen :inputs] conj (get-in $ [:parsed :temp-input])))
      (update :parsed dissoc :temp-input)))

(defn- parse-inputs
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp-ugen :inputs] [])
      (as-> $ (niterate $ parse-input (get-in $ [:parsed :temp-ugen :num-inputs])))))

(defn- parse-output
  [dv-reader]
  (-> dv-reader
      (get-uint8 (assocer :temp-output :calculation-rate))
      (as-> $ (update-in $ [:parsed :temp-ugen :outputs] conj (get-in $ [:parsed :temp-output])))
      (update :parsed dissoc :temp-output)))

(defn- parse-outputs
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp-ugen :outputs] [])
      (as-> $ (niterate $ parse-output (get-in $ [:parsed :temp-ugen :num-outputs])))))

(defn- parse-ugen
  [dv-reader]
  (-> dv-reader
      (get-pstring (assocer :temp-ugen :ugen-name))
      (get-uint8   (assocer :temp-ugen :calculation-rate))
      (get-uint32  (assocer :temp-ugen :num-inputs))
      (get-uint32  (assocer :temp-ugen :num-outputs))
      (get-uint16  (assocer :temp-ugen :special-index))
      (parse-inputs)
      (parse-outputs)
      (as-> $ (update-in $ [:parsed :temp :ugens] conj (get-in $ [:parsed :temp-ugen])))
      (update :parsed dissoc :temp-ugen)))

(defn- parse-ugen-specs
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp :ugens] [])
      (as-> $ (niterate $ parse-ugen (get-in $ [:parsed :temp :num-ugens])))))

(defn- parse-variant
  [dv-reader]
  (-> dv-reader
      (get-pstring (assocer :temp-variant :variant-name))
      (get-float32-array-via (assocer :temp-variant :init-variant-values) [:temp :num-params])
      (as-> $ (update-in $ [:parsed :temp :variants] conj (get-in $ [:parsed :temp-variant])))
      (update :parsed dissoc :temp-variant)))

(defn- parse-variant-specs
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp :variants] [])
      (as-> $ (niterate $ parse-variant (get-in $ [:parsed :temp :num-variants])))))

(defn- parse-synth-definition
  [dv-reader]
  (-> dv-reader
      (get-pstring (assocer :temp :synthdef-name))
      (get-uint32  (assocer :temp :num-constants))
      (get-float32-array-via (assocer :temp :constants) [:temp :num-constants])
      (get-uint32  (assocer :temp :num-params))
      (get-float32-array-via (assocer :temp :init-param-values) [:temp :num-params])
      (get-uint32  (assocer :temp :num-param-names))
      (parse-param-names)
      (get-uint32  (assocer :temp :num-ugens))
      (parse-ugen-specs)
      (get-uint16  (assocer :temp :num-variants))
      (parse-variant-specs)
      (as-> $ (update-in $ [:parsed :synthdefs] conj (get-in $ [:parsed :temp])))
      (update-in [:parsed] dissoc :temp)))

(defn- parse-synth-definitions
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :synthdefs] [])
      (niterate parse-synth-definition (get-in dv-reader [:parsed :num-synthdefs]))))

(defn- parse-synthdef-buffer
  [dv-reader]
  (try
    (-> dv-reader
        (parse-synthdef-header)
        (parse-synth-definitions)
        :parsed)
    (catch js/Error e
      (println (red (str e))))))

(defn parse-synthdef
  [path state]
  (read-file
   path
   (fn [err data]
     (if err
       (println (red err))
       (reset! state (parse-synthdef-buffer (data-view-reader (.-buffer data))))))))

(comment
  (def a (atom nil))
  (parse-synthdef "/home/bfay/.local/share/SuperCollider/synthdefs/sine.scsyndef" a)
  (parse-synthdef "/home/bfay/.local/share/SuperCollider/synthdefs/saw.scsyndef" a)
  (parse-synthdef "/home/bfay/.local/share/SuperCollider/synthdefs/lfsine.scsyndef" a)
  (parse-synthdef "/home/bfay/.local/share/SuperCollider/synthdefs/vartest.scsyndef" a)
  (deref a)
  
  )
