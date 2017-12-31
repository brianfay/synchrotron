(ns synchrotron.synthdef
  "Utils for reading a synthdef from binary format into a clojure map
  and writing from a clojure map into binary format"
  (:require [cljs.nodejs :as nodejs]
            [clojure.string :as string]
            [synchrotron.data-util :refer [get-uint8 get-uint32 get-int32 get-uint16
                                           get-pstring get-float32-array-via
                                           data-view-reader data-view-writer read-file
                                           write-data set-uint8 set-uint16 set-int32
                                           set-uint32 set-float32 set-pstring write-file]]
            [synchrotron.common-util :refer [red niterate assocer]]))

;;This code is like an etude in functional programming.
;;But it's one of those tricky etudes that's difficult to sight-read, doesn't sound very good,
;;and doesn't really teach anything useful.

(def SCgf 0x53436766);;magic 32-bit integer that supercollider uses to identity synthdef files

(defn- assert-file-type-id
  [file-type-id]
  (when (not= file-type-id SCgf)
    (throw (js/Error "First 4 bytes of synthdef file should be SCgf"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Parsing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn- parse-ugens
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

(defn- parse-variants
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :temp :variants] [])
      (as-> $ (niterate $ parse-variant (get-in $ [:parsed :temp :num-variants])))))

(defn- parse-synthdef
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
      (parse-ugens)
      (get-uint16  (assocer :temp :num-variants))
      (parse-variants)
      (as-> $ (update-in $ [:parsed :synthdefs] conj (get-in $ [:parsed :temp])))
      (update-in [:parsed] dissoc :temp)))

(defn- parse-synthdefs
  [dv-reader]
  (-> dv-reader
      (assoc-in [:parsed :synthdefs] [])
      (niterate parse-synthdef (get-in dv-reader [:parsed :num-synthdefs]))))

(defn- parse-synthdef-buffer
  [dv-reader]
  (try
    (-> dv-reader
        (parse-synthdef-header)
        (parse-synthdef)
        :parsed)
    (catch js/Error e
      (println (red (str e))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Counting
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- count-pstring-bytes
  "Count the bytes of a pascal-string representation of the given string s.
  Should just be the number of characters + 1"
  [s]
  (inc (count s)))

(defn- count-param-names-bytes
  [param-names]
  (reduce (fn [sum [s _]]
            (+ sum (count-pstring-bytes s) 4));;4 bytes for index in param array
          0
          param-names))

(defn- count-ugen-bytes
  [{:keys [ugen-name num-inputs num-outputs]}]
  (+ (count-pstring-bytes ugen-name)
     1 ;;calculation rate
     4 ;;num inputs
     4 ;;num outputs
     2 ;;special index
     (* num-inputs 8) ;;each input has two int32's
     num-outputs))    ;;each output has one int8

(defn- count-ugens-bytes
  [ugens]
  (reduce + 0 (map count-ugen-bytes ugens)))

(defn- count-variant-bytes
  [num-params variant]
  (+ (count-pstring-bytes (:variant-name variant))
     (* num-params 4))) ;;initial param values

(defn- count-variants-bytes
  [variants num-params]
  (reduce + 0 (map (partial count-variant-bytes num-params) variants)))

(defn- count-synthdef-bytes
  [{:keys [synthdef-name num-constants num-params param-names ugens variants]}]
  (+ ;;synthdef-name
     (count-pstring-bytes synthdef-name)
     ;;num-constants
     4
     ;;constant values
     (* 4 num-constants)
     ;;num-params
     4
     ;;init param values
     (* 4 num-params)
     ;;num-param-names
     4
     ;;param-names
     (count-param-names-bytes param-names)
     ;;num-ugens
     4
     (count-ugens-bytes ugens)
     ;;num-variants
     2
     ;;variants
     (count-variants-bytes variants num-params)))

(defn- count-synthdef-map-bytes
  "Takes a clojure map representation of a synth definition file
  and returns the number of bytes needed for binary file"
  [{:keys [num-synthdefs synthdefs]}]
  (let [constant-bytes 10] ;4 bytes for file-type-id + 4 bytes for file version + 2 bytes for number of synthdefs
    (reduce + 10 (map count-synthdef-bytes
                      (map #(nth synthdefs %) (range num-synthdefs))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Writing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- write-constants
  [dv-writer constants]
  (reduce (fn [acc v]
            (set-float32 acc v))
          dv-writer
          constants))

(defn- write-init-param-values
  [dv-writer init-params]
  (reduce (fn [acc v]
            (set-float32 acc v))
          dv-writer
          init-params))

(defn- write-param-names
  [dv-writer param-names]
  (reduce (fn [acc [k v]]
            (-> (set-pstring acc k)
                (set-uint32 v)))
          dv-writer
          param-names))

(defn- write-input
  [dv-writer {:keys [index-of-ugen index-of-output] :as input}]
  (-> dv-writer
      (set-uint32 index-of-ugen)
      (set-int32  index-of-output)))

(defn- write-inputs
  [dv-writer inputs]
  (reduce (fn [acc input] (write-input acc input))
          dv-writer
          inputs))

(defn- write-output
  [dv-writer {:keys [calculation-rate] :as output}]
  (-> dv-writer
      (set-uint8 calculation-rate)))

(defn- write-outputs
  [dv-writer outputs]
  (reduce (fn [acc output] (write-output acc output))
          dv-writer
          outputs))

(defn- write-variant
  [dv-writer {:keys [variant-name init-variant-values] :as variant} num-params]
  (-> dv-writer
      (set-pstring variant-name)
      (as-> $ (reduce (fn [acc v] (set-float32 acc v))
                      $
                      init-variant-values))))

(defn- write-variants
  [dv-writer variants num-params]
  (reduce (fn [acc variant] (write-variant acc variant num-params))
          dv-writer
          variants))

(defn- write-ugen
  [dv-writer {:keys [ugen-name calculation-rate num-inputs
                     num-outputs inputs outputs special-index] :as ugen}]
  (-> dv-writer
      (set-pstring    ugen-name)
      (set-uint8      calculation-rate)
      (set-uint32     num-inputs)
      (set-uint32     num-outputs)
      (set-uint16     special-index) ;;NOTE: I don't know if special-index is signed or not
      (write-inputs   inputs)
      (write-outputs  outputs)))

(defn- write-ugens
  [dv-writer ugens]
  (reduce (fn [acc ugen] (write-ugen acc ugen))
          dv-writer
          ugens))

(defn- write-synthdef
  "Writes the synthdef at index n to a buffer using the supplied dv-writer."
  [dv-writer {:keys [synthdef-name num-constants
                     constants num-params init-param-values
                     num-param-names param-names num-ugens ugens
                     num-variants variants] :as synthdef}]
  (-> dv-writer
      (set-pstring             synthdef-name)
      (set-uint32              num-constants)
      (write-constants         constants)
      (set-uint32              num-params)
      (write-init-param-values init-param-values)
      (set-uint32              num-param-names)
      (write-param-names       param-names)
      (set-uint32              num-ugens)
      (write-ugens             ugens)
      (set-uint16              num-variants)
      (write-variants          variants num-params)))

(defn- write-synthdefs
  [dv-writer synthdefs]
  (reduce write-synthdef dv-writer synthdefs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-synthdef-file
  "Takes the filepath to a binary synthdef file and reads it asynchronously into state, which should be an atom"
  [path state]
  (read-file
   path
   (fn [err data]
     (if err
       (println (red err))
       (reset! state (parse-synthdef-buffer (data-view-reader (.-buffer data))))))))

(defn write-synthdef-buffer
  "Takes a map representing a synthdef, returns a buffer with a binary representation that can be sent to the server"
  [{:keys [file-type-id file-version num-synthdefs synthdefs] :as synthdef-map}]
  (let [num-bytes (count-synthdef-map-bytes synthdef-map)
        dv-writer (data-view-writer num-bytes synthdef-map)]
    (-> dv-writer
        (set-uint32 file-type-id)
        (set-uint32 file-version)
        (set-uint16 num-synthdefs)
        (write-synthdefs synthdefs)
        :data-view
        .-buffer
        (js/Uint8Array.)
        (js/Buffer.))))
