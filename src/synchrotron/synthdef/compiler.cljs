(ns synchrotron.synthdef.compiler
  "Functions for taking a ugen graph and compiling it to an actual synthdef"
  (:require [clojure.set :refer [union]]
            [com.stuartsierra.dependency :as dep]
            [synchrotron.synthdef :as synthdef]
            [synchrotron.ugen :as ugen]))

(defn- ensure-constant
  "If the constant is not in constants, conj'es it in. Returns a vector with the
  constants vector and the index of the constant"
  [constants constant]
  (let [index-of-constant (.indexOf constants constant)]
    (case index-of-constant
      -1 [(conj constants constant) (count constants)]
      [constants index-of-constant])))

(defn- compile-inputs
  [ugen ugens constants]
  (let [partitioned-inputs (partition 2 (:inputs ugen))
        input-vals (map second partitioned-inputs)]
    (loop [inputs-to-read input-vals
           inputs []
           consts constants]
      (if-let [input (first inputs-to-read)]
        (if (map? input)
          ;;ugen
          (let [ancestor (some (fn [m] (when (= (:id m) (ffirst input)) m)) ugens)]
            (recur (rest inputs-to-read) (conj inputs {:index-of-ugen (.indexOf ugens ancestor)
                                                       :index-of-output (second (first input))}) consts))
          ;;constant
          (let [[constants-vec constant-idx] (ensure-constant consts input)]
            (recur (rest inputs-to-read) (conj inputs {:index-of-ugen -1 :index-of-output constant-idx}) constants-vec)))
        [inputs consts]))))

(defn- set-additional-ugen-fields
  "Sets num-inputs and special-index (if not already set)"
  [ugen]
  (-> (if (:special-index ugen) ugen (assoc ugen :special-index 0))
      (assoc :num-inputs (count (:inputs ugen)))))

(defn- compile-ugens
  [ugen-graph]
  (let [dep-graph (reduce (fn [g [n1 n2]] (dep/depend g n1 n2)) (dep/graph) (:dependencies ugen-graph))
        ordered-ugen-ids (dep/topo-sort dep-graph)]
    (loop [ugen-ids ordered-ugen-ids
           ugens     []
           constants []]
      (if-let [ugen-id (first ugen-ids)]
        (let [ugen (get-in ugen-graph [:ugens (first ugen-ids)])
              [inputs consts] (compile-inputs ugen ugens constants)]
          (recur (rest ugen-ids) (conj ugens (merge
                                              (select-keys ugen [:ugen-name :calculation-rate :id
                                                                 :num-outputs :outputs :special-index])
                                              {:inputs inputs})) consts))
        [(map set-additional-ugen-fields ugens) constants]))))

(defn compile-synthdef
  [synthdef-name]
  (let [ugen-list (deref ugen/ugens-in-current-synthdef)
        ugen-graph (reduce (fn [acc node]
                             {:ugens (merge (:ugens acc) (:ugens node))
                              :dependencies (union (:dependencies acc) (:dependencies node))})
                           {}
                           ugen-list)

        [ugens constants] (compile-ugens ugen-graph)]
    {:file-type-id synthdef/SCgf
     :file-version 2
     :num-synthdefs 1 ;;not supporting multiple synthdef definitions
     :synthdefs [{:ugens ugens
                  :synthdef-name synthdef-name
                  :num-ugens (count ugens)
                  :num-param-names 0 ;;TODO
                  :num-params 0 ;;TODO
                  :param-names [] ;;TODO
                  :init-param-values [] ;;TODO
                  :constants constants
                  :num-constants (count constants)
                  :num-variants 0 ;;not supporting variants yet; don't know any use cases
                  :variants []}]}))


