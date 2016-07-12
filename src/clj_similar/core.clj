(ns clj-similar.core
  (:require [clojure.set :as set]
            [kdtree :as kdtree])
  (:import [info.debatty.java.lsh MinHash]))

(defn index-set
  "Given a mapping of values to indexes and a set, returns a new set
  with the original values replaced by the corresponding indexes."
  [v->idx s]
  (reduce (fn [mem c] (conj mem (get v->idx c (int 0)))) (sorted-set) s))

(defn index-sets
  "Given a mapping of values to indexes and a collection of sets,
  returns a new collection of sets where each set has have their
  values replaced by indexes"
  [v->idx coll]
  (reduce (fn [mem c] (assoc mem c (index-set v->idx c))) {} coll))

(defn minhash-sets
  [hash-fn indexed-sets]
  (reduce (fn [mem [k v]]
            (conj mem (with-meta (into [] (hash-fn v)) {:value k}))) [] indexed-sets))

(defn value-index
  [v coll]
  (into {} (map-indexed (fn [idx itm] [itm (int (+ 1 idx))]) v)))

(defrecord Similar [v->idx tree hash-fn])

(defn- similar-internal
  [coll]
  (let [n (count coll)
        v (reduce set/union #{} coll)
        dict-size (count v)
        v->idx (value-index v coll)
        sets (index-sets v->idx coll)

        hasher (MinHash. (int n) (int dict-size))
        hash-fn #(.signature hasher %)
        h (minhash-sets hash-fn sets)
        tree (kdtree/build-tree h)]
    (Similar. v->idx tree hash-fn)))


;;; Public API
(defn similar
  "Constructs a new similar set from a collection of sets.
  Can be used for lookup of nearest sets using `nearest`"
  [coll]
  {:pre [(every? set? coll)]}
  (similar-internal coll))

(defn nearest
  ([similar s]
   {:pre [(set? s)]}
   (first (nearest similar s 1)))
  ([similar s n]
   {:pre [(set? s)]}
   (let [v->idx (:v->idx similar)
         s* (index-set v->idx s)
         h ((:hash-fn similar) s*)
         n (kdtree/nearest-neighbor (:tree similar) h n)
         mf (fn [e] (assoc (meta e) :dist-squared (:dist-squared e)))]
     (doall (map mf n)))))
