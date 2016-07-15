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
(def s (similar coll)) ;; default banding and stages are (3, 10)
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
"Elapsed time: 44252.975496 msecs"
Testing speed of nearest neighbor retrieval

Sample output for random target sets
in #{e E O m} out (#{e j O m} #{e O m}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})
in #{k} out (#{R k} #{k}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})
in #{d K M l u} out (#{d K M l} #{d K s M S l}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})
in #{M Y Z C r g X o} out (#{M S Z r g X o} #{Q L M g X o}) approximate ({:jaccard-index 1.0} {:jaccard-index 0.7777777777777778})
in #{w J M S Y E t b A} out (#{w Q q M t b N h} #{z w J M U b A}) approximate ({:jaccard-index 0.8888888888888888} {:jaccard-index 0.7777777777777778})
in #{u} out (#{u} #{R k u}) approximate ({:jaccard-index 1.0} {:jaccard-index 0.4444444444444444})
in #{L G h} out (#{G S h D} #{z L G Y h}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})
in #{e j M R F P} out (#{e j C F P y} #{e F P y u}) approximate ({:jaccard-index 1.0} {:jaccard-index 0.8888888888888888})
in #{a b} out (#{S a b y} #{R a b}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})
in #{n G Z V y u o} out (#{n G R V r} #{n G Z V U}) approximate ({:jaccard-index 1.0} {:jaccard-index 1.0})

Sample output for existing sets (with two elements omitted)
in #{} original #{l I} out () exact ()
in #{Z R P} original #{J Z R P c} out (#{Z R P} #{M Z P D}) exact ({:jaccard-index 1.0} {:jaccard-index 0.4})
in #{a O h D} original #{a O l A h D} out (#{S a O h D} #{a O r h W D}) exact ({:jaccard-index 0.8} {:jaccard-index 0.6666666666666666})
in #{n q R k r u} original #{n q R B k r l u} out (#{n q Y R r} #{n q R r A}) exact ({:jaccard-index 0.5714285714285714} {:jaccard-index 0.5714285714285714})
in #{n f p W} original #{n f p O i W} out (#{n z w s f p W} #{n f p j x J W c}) exact ({:jaccard-index 0.5714285714285714} {:jaccard-index 0.5})
in #{G i g m} original #{G t O i g m} out (#{G R i g m} #{p j G H i g m}) exact ({:jaccard-index 0.8} {:jaccard-index 0.5714285714285714})
in #{n z q J M I} original #{n K z q G J M I} out (#{z q J M I D} #{z J M I}) exact ({:jaccard-index 0.7142857142857143} {:jaccard-index 0.6666666666666666})
in #{j} original #{f j r} out (#{j} #{z j y A}) exact ({:jaccard-index 1.0} {:jaccard-index 0.25})
in #{T d} original #{T d g N} out (#{T d} #{T d u}) exact ({:jaccard-index 1.0} {:jaccard-index 0.6666666666666666})
in #{c} original #{Q O c} out (#{c} #{T J c}) exact ({:jaccard-index 1.0} {:jaccard-index 0.3333333333333333})
Evaluation count : 7680 in 60 samples of 128 calls.
             Execution time mean : 7.905725 ms
    Execution time std-deviation : 271.636864 µs
   Execution time lower quantile : 7.457396 ms ( 2.5%)
   Execution time upper quantile : 8.457490 ms (97.5%)
                   Overhead used : 9.210622 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 20.6311 % Variance is moderately inflated by outliers

```

Benchmarks were run on OS X El Capitan (Intel Xeon E3-1240V2 @ 3.4Ghz, 32 GB DDR3 RAM)

## Dependencies

* [java-lsh](https://github.com/tdebatty/java-LSH)
* [K-d tree by Levy](http://home.wlu.edu/~levys/software/kd/)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
