(ns clj-similar.core
  (:require [clojure.set :as set])
  (:import [info.debatty.java.lsh MinHash]
           [edu.wlu.cs.levy.CG KDTree]
           [java.util TreeSet Set]))

(defn index-set
  [v->idx s]
  (let [ts (TreeSet.)]
    (doseq [c s]
      (.add ^TreeSet ts (get v->idx c (int 0))))
    ts))

(defn index-sets
  "Given a mapping of values to indexes and a collection of sets,
  returns a new collection of sets where each set has have their
  values replaced by indexes"
  [v->idx coll]
  (reduce (fn [mem c] (assoc mem (index-set v->idx c) c)) {} coll))

(defn value-index
  [v]
  (into {} (map-indexed (fn [idx itm] [itm (int (inc idx))]) v)))

(defn points
  [hash-fn indexed-sets]
  (let [rf (fn [mem [ts k]]
             (let [hash (hash-fn ts)
                   hash-vec (vec hash)
                   ;; Hash bucket
                   bucket (get mem hash-vec [])]
               (assoc mem hash-vec (conj bucket {:value k :s ts :sig hash}))))]
    (reduce rf {} indexed-sets)))


(defrecord Similar [v->idx tree hash-fn minhash])

(defn build-tree
  [points size]
  (let [^KDTree tree (KDTree. size)]
    (doseq [point points]
      (.insert tree
               ^doubles (double-array (first point))
               ^Object (second point)))
    tree))

(defn- similar-internal
  [coll error]
  (let [n (count coll)
        v (reduce set/union #{} coll)
        dict-size (count v)
        v->idx (value-index v)
        sets (index-sets v->idx coll)
        size (MinHash/size error)
        minhash (MinHash. ^int (int size) ^int (int dict-size))
        hash-fn #(.signature ^MinHash minhash ^Set %)
        tree (build-tree (points hash-fn sets) size)]
    (Similar. v->idx tree hash-fn minhash)))


;;; Public API
(defn similar
  "Constructs a new similar set from a collection of sets.
  Can be used for lookup of nearest sets using `nearest`.
  Optionally takes a target similarity estimate error (default 0.1)."
  ([coll]
   {:pre [(every? set? coll)]}
   (similar-internal coll 0.1))
  ([coll error]
   {:pre [(every? set? coll)]}
   (similar-internal coll error)))

(defn jaccard-index
  [^Set s1 ^Set s2]
  (MinHash/JaccardIndex s1 s2))

(defn approximate-jaccard-index
  [^MinHash minhash ^ints sig1 ^ints sig2]
  (.similarity minhash sig1 sig2))

(defn similarity
  [exact? minhash sig1 sig2 s1 s2]
  (if exact?
    (jaccard-index s1 s2)
    (approximate-jaccard-index minhash sig1 sig2)))

(defn nearest
  "Given a `similar` data structure and a target set `s`, finds the
  nearest matching set. Optionally takes a parameter `n` for the `n`
  nearest sets. Pass `threshold` as an optional arguments to filter
  elements with a jaccard index below the threshold. By default this
  is calculated heuristically using the MinHash, pass `exact? true` to
  use the true jaccard index instead. Returning sets have distance
  metrics and vector associated as metadata."
  ([similar s]
   {:pre [(set? s)]}
   (first (nearest similar s 1)))
  ([similar s n & {:keys [threshold exact?] :or {threshold 0.0 exact? false}}]
   {:pre [(set? s)]}
   (let [v->idx (:v->idx similar)
         s* (index-set v->idx s)
         sig* ((:hash-fn similar) s*)
         ff #(> (:jaccard-index (meta %)) threshold)
         similarity* (partial similarity exact? (:minhash similar))
         mf (fn [e]
              (let [ji (similarity* sig* (:sig e) s* (:s e))]
                (with-meta (:value e) {:jaccard-index ji})))
         n (.nearest ^KDTree (:tree similar) ^doubles (double-array sig*) ^int n)
         sf #(:jaccard-index (meta %))]

     (take
      (filter ff (reverse (sort-by sf (map mf (flatten (vec n))))))
      n))))
