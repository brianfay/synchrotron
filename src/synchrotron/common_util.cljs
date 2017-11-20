(ns synchrotron.common-util
  "Generic helper functions that may come in handy in multiple places.")

(defn green
  "Encode string with ANSI escape sequence to make it green."
  [s]
  (str "\033[32m" s "\033[m"))

(defn red
  "Encode string with ANSI escape sequence to make it red."
  [s]
  (str "\033[31m" s "\033[m"))

(defn- assocer
  "Returns a function that assoc's using the given key or collection of keys."
  [k & ks]
  (if ks
    (fn [m v] (assoc-in m (cons k ks) v))
    (fn [m v] (assoc m k v))))

(defn niterate
  "Recursively call f n times, with the initial value init."
  [init f n]
  (nth (iterate f init) n))
