(ns clj-similar.core
  (:require [clojure.set :as set])
  (:import [info.debatty.java.lsh MinHash LSHMinHash]
           [edu.wlu.cs.levy.CG KDTree]
           [org.apache.commons.lang3 ArrayUtils]
           [java.util TreeSet Set Collection]))

(defn index-array
  [dict size s]
  (let [lst (boolean-array (take size (repeat false)))
        idx (map (fn [e] (int (get dict e 0))) s)]
    (doseq [i idx]
      (aset-boolean lst i true))
    lst))

(defn index-sets
  "Given a mapping of values to indexes and a collection of sets,
  returns a new collection of sets where each set has have their
  values replaced by indexes"
  [dict size coll]
  (reduce (fn [mem s] (assoc mem (index-array dict size s) s)) {} coll))

(defn value-index
  [v]
  (into {} (map-indexed (fn [idx itm] [itm (inc idx)]) v)))

(defn points
  [hash-fn indexed-sets]
  (let [rf (fn [mem [ia k]]
             (let [hash (hash-fn ia)
                   hash-vec (vec hash)
                   ;; Hash bucket
                   bucket (get mem hash-vec [])]
               (assoc mem hash-vec (conj bucket {:ia ia :s k}))))]
    (reduce rf {} indexed-sets)))


(defrecord Similar [dict size tree hash-fn mh])

(defn build-tree
  [points size]
  (let [^KDTree tree (KDTree. size)]
    (doseq [point points]
      (.insert tree
               ^doubles (double-array (first point))
               ^Object (second point)))
    tree))

(defn get-field
  "Returns obj's private or public field with given field-name,
     defined in klass. Pass nil into obj for static fields."
  [klass field-name obj]
  (-> klass (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(defn- similar-internal
  [coll buckets stages]
  (let [n (count coll)
        dict (value-index (reduce set/union #{} coll))
        size (inc (count dict))
        sets (index-sets dict size coll)
        lsh (LSHMinHash. ^int (int stages) ^int (int buckets) ^int (int size))
        mh (get-field LSHMinHash "mh" lsh) ;; why oh why is this private.
        hash-fn #(.hash ^LSHMinHash lsh %)
        tree (build-tree (points hash-fn sets) stages)]
    (Similar. dict size tree hash-fn mh)))


;;; Public API
(defn similar
  "Constructs a new similar set from a collection of sets.
  Can be used for lookup of nearest sets using `nearest`.
  Optionally takes `bucket` and `stages` (also known as bands) as arguments."
  ([coll]
   (similar coll 10 3))
  ([coll buckets]
   (similar coll buckets 3))
  ([coll buckets stages]
   {:pre [(every? set? coll)]}
   (similar-internal coll buckets stages)))


(defn jaccard-index
  [^Set s1 ^Set s2]
  (MinHash/JaccardIndex s1 s2))

(defn approximate-jaccard-index
  [^MinHash mh ^booleans ia1 ^booleans ia2]
  (.similarity mh (.signature mh ia1) (.signature mh ia2)))

(defn similarity
  [exact? minhash s1 s2 ia1 ia2]
  (if exact?
    (jaccard-index s1 s2)
    (approximate-jaccard-index minhash ia1 ia2)))


(defn nearest
  "Given a `similar` data structure and a target set `s`, finds the
  nearest matching set. Optionally takes a parameter `n` for the `n`
  nearest sets. Pass `threshold` as an optional arguments to filter
  elements with a jaccard index below the threshold.
  Returning sets have distance metrics and vector associated as metadata."
  ([similar s]
   {:pre [(set? s)]}
   (first (nearest similar s 1)))
  ([similar s n & {:keys [threshold exact?] :or {threshold 0.0 exact? true}}]
   {:pre [(set? s)]}
   (let [{dict :dict
          size :size
          hash-fn :hash-fn
          mh :mh} similar
         ia (index-array dict size s)
         hash ((:hash-fn similar) ia)
         ff #(> (:jaccard-index (meta %)) threshold)
         mf (fn [e]
              (let [ji (similarity exact? mh s (:s e) ia (:ia e))]
                (with-meta (:s e) {:jaccard-index ji})))
         nearest (.nearest ^KDTree (:tree similar) ^doubles (double-array hash) ^int n)
         sf #(:jaccard-index (meta %))]
     (take n (filter ff (reverse (sort-by sf (distinct (map mf (flatten (vec nearest)))))))))))
