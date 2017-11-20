(ns synchrotron.data-util
  (:require [synchrotron.common-util :refer [niterate]]))

(defn data-view-reader
  "Abstraction for reading a buffer. The idea is that you will read some data, use it to build parsed, then increase the offset. Rinse and repeat until you've got the output you need"
  [buffer]
  {:offset 0
   :data-view (js/DataView. buffer)
   :parsed nil})

(defn get-data-from-dv-reader
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

(defn get-array-from-dv-reader
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

(defn get-float32-array-via
  [data-reader update-fn path-to-source]
  (get-float32-array data-reader
                     update-fn
                     (get-in data-reader (cons :parsed path-to-source))))

(defn build-string [data-view offset nbytes]
  (reduce (fn [coll offset]
            (str coll (char (.getUint8 data-view offset))))
          ""
          (range offset (+ nbytes offset))))

(defn get-pstring
  "Read a pascal string from the data-view-reader Update the data-view-reader
  with the value of the string, using update-fn"
  [{:keys [data-view offset parsed] :as dv-reader} update-fn]
  (let [nbytes (.getUint8 data-view offset)
        string (build-string data-view (inc offset) nbytes)]
    (assoc dv-reader
           :offset (+ offset (inc nbytes))
           :parsed (update-fn parsed string))))
