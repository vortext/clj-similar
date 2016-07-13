(ns clj-similar.core
  (:require [clojure.set :as set]
            [kdtree :as kdtree])
  (:import [info.debatty.java.lsh MinHash]
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
  (reduce (fn [mem c] (assoc mem c (index-set v->idx c))) {} coll))

(defn minhash-sets
  [hash-fn indexed-sets]
  (let [mf (fn [[k ts]]
             (with-meta (hash-fn ts)
               {:value k :s ts}))]
    (map mf indexed-sets)))

(defn value-index
  [v]
  (into {} (map-indexed (fn [idx itm] [itm (int (inc idx))]) v)))

(defrecord Similar [v->idx tree hash-fn])

(defn- similar-internal
  [coll error]
  (let [n (count coll)
        v (reduce set/union #{} coll)
        dict-size (count v)
        v->idx (value-index v)
        sets (index-sets v->idx coll)

        hasher (MinHash. (double error) (int dict-size))
        hash-fn #(vec (.signature ^MinHash hasher ^Set %))
        h (minhash-sets hash-fn sets)
        tree (kdtree/build-tree h)]
    (Similar. v->idx tree hash-fn)))


;;; Public API
(defn similar
  "Constructs a new similar set from a collection of sets.
  Can be used for lookup of nearest sets using `nearest`.
  Optionally takes a target similarity estimate error (default 0.05)."
  ([coll]
   {:pre [(every? set? coll)]}
   (similar-internal coll 0.05))
  ([coll error]
   {:pre [(every? set? coll)]}
   (similar-internal coll error)))

(defn jaccard-index
  [^Set s1 ^Set s2]
  (MinHash/JaccardIndex s1 s2))

(defn nearest
  "Given a `similar` data structure and a target set `s`, finds the nearest matching set.
   Optionally takes a parameter `n` for the `n` nearest sets.
   Returning sets have distance metrics and vector associated as metadata."
  ([similar s]
   {:pre [(set? s)]}
   (first (nearest similar s 1)))
  ([similar s n & {:keys [threshold] :or {threshold 0.0}}]
   {:pre [(set? s)]}
   (let [v->idx (:v->idx similar)
         s* (index-set v->idx s)
         h ((:hash-fn similar) s*)
         n (kdtree/nearest-neighbor (:tree similar) h n)
         ff #(>= (:jaccard-index (meta %)) threshold)
         mf (fn [e]
              (let [m (meta e)
                    ji (jaccard-index s* (:s m))]
                (with-meta (:value m) (assoc e :jaccard-index ji))))]
     (filter ff (map mf n)))))
