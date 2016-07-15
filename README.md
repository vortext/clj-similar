# clj-similar
[![Clojars Project](https://img.shields.io/clojars/v/clj-similar.svg)](https://clojars.org/clj-similar) [![Build Status](https://travis-ci.org/vortext/clj-similar.png?branch=develop)](https://travis-ci.org/vortext/clj-similar)


Experimental library for (fast) approximate similar set lookup.
Under the hood it uses a [MinHash](https://en.wikipedia.org/wiki/MinHash) to compute [locality sensitive hashes](https://en.wikipedia.org/wiki/Locality-sensitive_hashing) of a collection of sets, and loads them into a [k-d tree](https://en.wikipedia.org/wiki/K-d_tree).
The constructed `similar` probabilistic data structure can be used to retrieve [nearest neighbors](https://en.wikipedia.org/wiki/Nearest_neighbor_search) of a given target set.
While the construction of the data structure can be expensive, lookups should be fast.
The results are non-deterministic, and depend on the bucket and banding sizes.

Note that it will always return some set that is considered nearest.
The resulting sets can optionally be filtered by their [Jaccard index](https://en.wikipedia.org/wiki/Jaccard_index), allowing to omit values that are too dissimilar.

## Caveats
- Collections of sets that have more than the maximum integer value of distinct values are currently unsupported.
- The data structure is read-only, support for modifications is not currently planned (but pull requests welcome).
- Read the [binning caveats](https://github.com/tdebatty/java-LSH#binning) before using!
- The performance is *very* sensitive to the `bucket` and `stages` parameters, [LSH forests](http://www.cs.princeton.edu/courses/archive/spr06/cos592/bib/LSHForest-bawa05.pdf) might provide a solution, but are currently not implemented.

## Usage
Note, LSH using MinHash is very sensitive to the average Jaccard similarity in your dataset! If most vectors in your dataset have a Jaccard similarity above or below 0.5, they might all fall in the same bucket. The example below might thus give different results for each run.

```clojure
(require '[clj-similar.core :refer [similar nearest]])
(def coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}])
;; Creates the data structure

;; the number of buckets should be chosen such that we have at least 100 items per bucket
(def buckets 10)
;; the number of stages is also sometimes called the number of bands
(def stages 3)
(def s (similar coll)) ;; default buckets and stages are (10, 3)
(def s (similar coll buckets stages))

;; A single nearest neighbor
(nearest s #{"f" "e" "a" "b"})
;=> #{"f" "e" "a" "b"}

(nearest s #{"f" "e" "a" "b" "x"})
;=> #{"f" "e" "a" "b"}

(nearest s #{"f" "e" "a"})
;=> #{"f" "e" "a" "b"}

(nearest s #{"a"})
;=> #{"a" "b" "c"}

;; Two nearest neighbors
(nearest s #{"a" "b"} 2)
;=> (#{"a" "b" "c"} #{"f" "e" "a" "b"})

;; To access the distance metrics use the associated metadata
(:jaccard-index (meta (nearest s #{"a" "b"})))

;; Or you can optionally filter values below a certain jaccard-index threshold
(nearest s #{"a" "b"} 2 :threshold 0.6)
;=> (#{"a" "b" "c"})

;; The values of the sets can be any Clojure data structure, even other collections
(def coll [#{["a"] ["a" "b"]} #{["c" "d"] ["a" "c"]}])
(def s (similar coll))
(nearest s #{["a" "b"]})
;=> #{["a" "b"] ["a"]}

```

## Benchmark
```
Generating 1000000 random sets with max-size 10
Generating similar data structure
"Elapsed time: 44071.000835 msecs"
Testing speed of nearest neighbor retrieval
Sample output for random target sets
in #{Y R a g l W c} out (#{q Y R t g X l W} #{Y Z R a g N}) approximate ({:jaccard-index 0.5} {:jaccard-index 0.4444444444444444})
in #{d w Z X N D} out (#{Z X N D} #{d Z a X D}) approximate ({:jaccard-index 0.6666666666666666} {:jaccard-index 0.5714285714285714})
in #{w q Z H B i A I} out (#{e q M H R B i I} #{q x M H B a i A}) approximate ({:jaccard-index 0.45454545454545453} {:jaccard-index 0.45454545454545453})
in #{w p U b N o} out (#{w U b N o} #{U b N}) approximate ({:jaccard-index 0.8333333333333334} {:jaccard-index 0.5})
in #{B D} out (#{B D} #{E B D}) approximate ({:jaccard-index 1.0} {:jaccard-index 0.6666666666666666})
in #{B V O c} out (#{T K B V h c} #{v B V i m c}) approximate ({:jaccard-index 0.42857142857142855} {:jaccard-index 0.42857142857142855})
in #{F a V l} out (#{w F a V l} #{F a V l N}) approximate ({:jaccard-index 0.8} {:jaccard-index 0.8})
in #{d f x J S k l u D} out (#{x J S E l u D} #{d L x J v S C y u D}) approximate ({:jaccard-index 0.6} {:jaccard-index 0.46153846153846156})
in #{w s q v a P O i} out (#{v a P i} #{q v O i D}) approximate ({:jaccard-index 0.5} {:jaccard-index 0.4444444444444444})
in #{d j Z R O k D} out (#{j Z a O D} #{d e j x Z B O D}) approximate ({:jaccard-index 0.5} {:jaccard-index 0.5})
Sample output for existing sets
in #{E U O i g l A D} original #{L E U O i g l A I D} out (#{L E U O i g l A I D} #{e t U i g l A D}) exact ({:jaccard-index 0.8} {:jaccard-index 0.6})
in #{n i h} original #{n U i k h} out (#{n i h} #{n i l h}) exact ({:jaccard-index 1.0} {:jaccard-index 0.75})
in #{p v R y D} original #{p v R B y X D} out (#{q p v R y N D} #{p R D}) exact ({:jaccard-index 0.7142857142857143} {:jaccard-index 0.6})
in #{n q x G U} original #{n q x G C U W} out (#{n x G U} #{d n q p x G U}) exact ({:jaccard-index 0.8} {:jaccard-index 0.7142857142857143})
in #{v k b y m} original #{w v B k b y m} out (#{w v B k b y m} #{v k y A m D}) exact ({:jaccard-index 0.7142857142857143} {:jaccard-index 0.5714285714285714})
in #{l} original #{f l D} out (#{l} #{X l}) exact ({:jaccard-index 1.0} {:jaccard-index 0.5})
in #{P b} original #{f S P b} out (#{P b} #{q P b}) exact ({:jaccard-index 1.0} {:jaccard-index 0.6666666666666666})
in #{f} original #{f W D} out (#{f} #{f N}) exact ({:jaccard-index 1.0} {:jaccard-index 0.5})
in #{U b} original #{U O b l} out (#{U b} #{U b o}) exact ({:jaccard-index 1.0} {:jaccard-index 0.6666666666666666})
in #{T E V l D} original #{T E B V U l D} out (#{T E a V D} #{V O l D}) exact ({:jaccard-index 0.6666666666666666} {:jaccard-index 0.5})
Evaluation count : 8700 in 60 samples of 145 calls.
             Execution time mean : 6.829506 ms
    Execution time std-deviation : 323.983327 µs
   Execution time lower quantile : 6.341004 ms ( 2.5%)
   Execution time upper quantile : 7.483967 ms (97.5%)
                   Overhead used : 9.201927 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 33.5825 % Variance is moderately inflated by outliers

```

Benchmarks were run on OS X El Capitan (Intel Xeon E3-1240V2 @ 3.4Ghz, 32 GB DDR3 RAM)

## Dependencies

* [java-lsh](https://github.com/tdebatty/java-LSH)
* [K-d tree by Levy](http://home.wlu.edu/~levys/software/kd/)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
