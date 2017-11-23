(ns synchrotron.data-util
  (:require [cljs.nodejs :as nodejs]
            [synchrotron.common-util :refer [niterate]]))

(defonce fs (nodejs/require "fs"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reading
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn read-file [path cb]
  (.readFile fs path cb))

(defn data-view-reader
  "Abstraction for reading a buffer. The idea is that you will read some data, use it to build parsed, then increase the offset. Rinse and repeat until you've got the output you need"
  [buffer]
  {:offset 0
   :data-view (js/DataView. buffer)
   :parsed nil})

(defn- get-data-from-dv-reader
  "Reads data from a data-view-reader and returns the new data-view-reader
   Data is read by read-fn at the current offset and used to update parsed.
   Offset is increased by num-bytes"
  [read-fn num-bytes {:keys [data-view offset parsed] :as dv-reader} update-fn]
  (assoc dv-reader
         :offset (+ offset num-bytes)
         :parsed (update-fn parsed (read-fn data-view offset))))

(def get-int8    (partial get-data-from-dv-reader #(.getInt8    %1 %2) 1))
(def get-uint8   (partial get-data-from-dv-reader #(.getUint8   %1 %2) 1))
(def get-int16   (partial get-data-from-dv-reader #(.getInt16   %1 %2) 2))
(def get-uint16  (partial get-data-from-dv-reader #(.getUint16  %1 %2) 2))
(def get-int32   (partial get-data-from-dv-reader #(.getInt32   %1 %2) 4))
(def get-uint32  (partial get-data-from-dv-reader #(.getUint32  %1 %2) 4))
(def get-float32 (partial get-data-from-dv-reader #(.getFloat32 %1 %2) 4))
(def get-float64 (partial get-data-from-dv-reader #(.getFloat64 %1 %2) 8))

(defn- get-array-from-dv-reader
  [read-fn data-reader update-fn n-items]
  (-> data-reader
      (assoc-in [:parsed :tmp-array-builder] [])
      (niterate (fn [data-reader]
                  (read-fn data-reader #(update-in %1 [:tmp-array-builder] conj %2)))
                n-items)
      (as-> $ (update $ :parsed update-fn (get-in $ [:parsed :tmp-array-builder])))
      (update :parsed dissoc :tmp-array-builder)))

(def get-int8-array    (partial get-array-from-dv-reader get-int8))
(def get-uint8-array   (partial get-array-from-dv-reader get-uint8))
(def get-int16-array   (partial get-array-from-dv-reader get-int16))
(def get-uint16-array  (partial get-array-from-dv-reader get-uint16))
(def get-int32-array   (partial get-array-from-dv-reader get-int32))
(def get-uint32-array  (partial get-array-from-dv-reader get-uint32))
(def get-float32-array (partial get-array-from-dv-reader get-float32))
(def get-float64-array (partial get-array-from-dv-reader get-float64))

(defn- get-float32-array-via
  [data-reader update-fn path-to-source]
  (get-float32-array data-reader
                     update-fn
                     (get-in data-reader (cons :parsed path-to-source))))

(defn- build-string [data-view offset nbytes]
  (reduce (fn [coll offset]
            (str coll (char (.getUint8 data-view offset))))
          ""
          (range offset (+ nbytes offset))))

(defn get-pstring
  "Read a pascal string from the data-view-reader. Update the data-view-reader
  with the value of the string, using update-fn"
  [{:keys [data-view offset parsed] :as dv-reader} update-fn]
  (let [nbytes (.getUint8 data-view offset)
        string (build-string data-view (inc offset) nbytes)]
    (assoc dv-reader
           :offset (+ offset (inc nbytes))
           :parsed (update-fn parsed string))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Writing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn write-file [path array-buffer]
  (.writeFile fs path (.from js/Buffer array-buffer)))

(defn data-view-writer
  "Abstraction for writing data to a buffer.
  The data that will be written to the buffer should be provided in source-data.
  The data-view-writer will be threaded through various functions, which will
  write to the buffer and increase the offset."
  [num-bytes source-data]
  {:offset 0
   :source-data source-data
   :data-view (js/DataView. (js/ArrayBuffer. num-bytes))})

(defn- write-data
  "Gets data from source-data by applying getter-fns (in left-to-right order).
  Writes this data to the data-view-writer using setter-fn, which should increment the offset."
  [{:keys [offset source-data data-view] :as dv-writer} setter-fn & getter-fns]
  (def dv-writer dv-writer)
  (def setter-fn setter-fn)
  (def getter-fns getter-fns)
  (def source-data source-data)
  (let [getter-fn (apply comp (reverse getter-fns))
        data      (getter-fn source-data)]
    (def data data)
    (setter-fn dv-writer data)))


(defn- set-data-for-dv-writer
  "Writes data with the write-fn using data-view-writer and returns the data-view-writer
  updated with the new offset incremented by num-bytes."
  [write-fn num-bytes {:keys [data-view offset] :as dv-writer} data]
  (write-fn data-view offset data)
  (assoc dv-writer
         :offset (+ offset num-bytes)))

(def set-int8    (partial set-data-for-dv-writer #(.setInt8    %1 %2 %3) 1))
(def set-uint8   (partial set-data-for-dv-writer #(.setUint8   %1 %2 %3) 1))
(def set-int16   (partial set-data-for-dv-writer #(.setInt16   %1 %2 %3) 2))
(def set-uint16  (partial set-data-for-dv-writer #(.setUint16  %1 %2 %3) 2))
(def set-int32   (partial set-data-for-dv-writer #(.setInt32   %1 %2 %3) 4))
(def set-uint32  (partial set-data-for-dv-writer #(.setUint32  %1 %2 %3) 4))
(def set-float32 (partial set-data-for-dv-writer #(.setFloat32 %1 %2 %3) 4))
(def set-float64 (partial set-data-for-dv-writer #(.setFloat64 %1 %2 %3) 8))

(defn set-pstring
  [{:keys [data-view offset] :as dv-writer} s]
  (let [num-bytes (count s)]
    (-> (set-uint8 dv-writer num-bytes)
        (as-> $ (reduce (fn [accum x]
                          (set-uint8 accum (.charCodeAt x 0))) $ s)))))

(comment

  (count-bytes
   {:num-things 4
    :things [0 1 2 3]}
   [:uint8   :num-things]
   [:uint8   :things first]
   [[thing-spec :num-things] :things])

  (concat [[:uint8   :num-things]
           [:uint8   :things first]]
          (take 3 (repeat [:foo])))


  (.charCodeAt "a" 0)
  (char 0x439)
  (.charCodeAt "awefa" 0)

  (.charCodeAt (char "a") 0)
  (.charCodeAt (first (map identity "foo")) 0)

  (map int "foo")
  [set-int8 ]
  (.getInt8
   (-> (let [dv-writer (data-view-writer (js/ArrayBuffer. 8) {:foo {:bar {:baz 4}}})]
         (write-data dv-writer set-int8 :foo :bar :baz))
       :data-view
       ) 0)

  (.getInt8 (let [dv-writer (data-view-writer (js/ArrayBuffer. 8) {:foo {:bar {:baz 4}}})]
     (-> (set-pstring dv-writer "foo")
         :data-view 
         )) 4)

  (-> (set-int8 (data-view-writer (js/ArrayBuffer. 8) {:foo {:bar {:baz 4}}}) 10)
      :data-view
      (.getInt8 0))
  (let [dv (js/DataView. (js/ArrayBuffer. 8))]
      (.setInt32 dv 0 500)
      (.setInt32 dv 4 501)
      (.getInt32 dv 4)
      )
  )
