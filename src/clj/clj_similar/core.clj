(ns clj-similar.core
  (:require [clojure.set :as set])
  (:import [info.debatty.java.lsh MinHash LSHMinHash]
           [edu.wlu.cs.levy.CG KDTree]
           [java.util TreeSet Set Collection ArrayList]))

(defn index-array
  [dict s]
  (let [size (inc (count dict))
        lst (ArrayList. ^Collection (take size (repeat false)))]
    (doseq [c s]
      (.set lst (int (get dict c 0)) true))
    (boolean-array lst)))

(defn index-sets
  "Given a mapping of values to indexes and a collection of sets,
  returns a new collection of sets where each set has have their
  values replaced by indexes"
  [dict coll]
  (reduce (fn [mem s] (assoc mem (index-array dict s) s)) {} coll))

(defn value-index
  [v]
  (into {} (map-indexed (fn [idx itm] [itm (inc idx)]) v)))

(defn points
  [hash-fn indexed-sets]
  (let [rf (fn [mem [ts k]]
             (let [hash (hash-fn ts)
                   hash-vec (vec hash)
                   ;; Hash bucket
                   bucket (get mem hash-vec [])]
               (assoc mem hash-vec (conj bucket {:value k}))))]
    (reduce rf {} indexed-sets)))


(defrecord Similar [dict tree hash-fn])

(defn build-tree
  [points size]
  (let [^KDTree tree (KDTree. size)]
    (doseq [point points]
      (.insert tree
               ^doubles (double-array (first point))
               ^Object (second point)))
    tree))

(defn- similar-internal
  [coll buckets stages]
  (let [n (count coll)
        dict (value-index (reduce set/union #{} coll))
        sets (index-sets dict coll)
        size (inc (count dict))
        lsh (LSHMinHash. ^int (int stages) ^int (int buckets) ^int (int size))
        hash-fn #(.hash ^LSHMinHash lsh %)
        tree (build-tree (points hash-fn sets) stages)]
    (Similar. dict tree hash-fn)))


;;; Public API
(defn similar
  "Constructs a new similar set from a collection of sets.
  Can be used for lookup of nearest sets using `nearest`.
  Optionally takes `bucket` and `stages` (also known as bands) as arguments."
  ([coll]
   (similar coll 10 2))
  ([coll buckets]
   (similar coll buckets 2))
  ([coll buckets stages]
   {:pre [(every? set? coll)]}
   (similar-internal coll buckets stages)))

(defn jaccard-index
  [^Set s1 ^Set s2]
  (MinHash/JaccardIndex s1 s2))

(defn nearest
  "Given a `similar` data structure and a target set `s`, finds the
  nearest matching set. Optionally takes a parameter `n` for the `n`
  nearest sets. Pass `threshold` as an optional arguments to filter
  elements with a jaccard index below the threshold.
  Returning sets have distance metrics and vector associated as metadata."
  ([similar s]
   {:pre [(set? s)]}
   (first (nearest similar s 1)))
  ([similar s n & {:keys [threshold] :or {threshold 0.0}}]
   {:pre [(set? s)]}
   (let [dict (:dict similar)
         hash ((:hash-fn similar) (index-array dict s))
         ff #(> (:jaccard-index (meta %)) threshold)
         mf (fn [e]
              (let [ji (jaccard-index s (:value e))]
                (with-meta (:value e) {:jaccard-index ji})))
         nearest (.nearest ^KDTree (:tree similar) ^doubles (double-array hash) ^int n)
         sf #(:jaccard-index (meta %))]
     (println nearest)
     (take n (filter ff (reverse (sort-by sf (map mf (flatten (vec nearest))))))))))
