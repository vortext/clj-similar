# clj-similar
[![Clojars Project](https://img.shields.io/clojars/v/clj-similar.svg)](https://clojars.org/clj-similar) [![Build Status](https://travis-ci.org/vortext/clj-similar.png?branch=develop)](https://travis-ci.org/vortext/clj-similar)


Experimental library for (fast) similar set lookup.
Under the hood it uses a [MinHash](https://en.wikipedia.org/wiki/MinHash) to compute [locality sensitive hashes](https://en.wikipedia.org/wiki/Locality-sensitive_hashing) of a collection of sets, and loads them into a [k-d tree](https://en.wikipedia.org/wiki/K-d_tree).
The constructed `similar` data structure can be used to retrieve [nearest neighbors](https://en.wikipedia.org/wiki/Nearest_neighbor_search) of a given target set.
While the construction of the data structure can be expensive, lookups should be fast.

Note that it will always return some set that is considered nearest.
The resulting sets can optionally be filtered by their (real) jaccard-index, allowing to omit values that are too dissimilar.

## Caveats
- Collections of sets that have more than the maximum integer value of distinct values are currently unsupported.
- The data structure is read-only, support for modifications is not currently planned (but pull requests welcome).
- The speed of the construction and lookup is almost completely determined by the similarity estimation error, higher errors are faster.

## Usage

```clojure
(require '[clj-similar.core :refer [similar nearest]])
(def coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}])
;; Creates the data structure
(def s (similar coll)) ;; default similarity estimation error (0.01)
(def s (similar coll 0.05)) ;; with a given similarity estimation error.

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

;; To access the distance metrics and computed point use the associated metadata
;; e.g. jaccard-index
(:jaccard-index (meta (nearest s #{"a" "b"})))

;; Or you can optionally filter values below a certain jaccard-index threshold
(nearest s #{"a" "b"} 2 :threshold 0.6)
;=> (#{"a" "b" "c"})

;; By default this uses the approximate jaccard-index from the MinHash values
;; You can calculate the exact jaccard indexes instead by passing exact? true
(nearest s #{"a" "b"} 2 :threshold 0.6 :exact? true)

;; The values of the sets can be any Clojure data structure, even other collections
(def coll [#{["a"] ["a" "b"]} #{["c" "d"] ["a" "c"]}])
(def s (similar coll))
(nearest s #{["a" "b"]})
;=> #{["a" "b"] ["a"]}

```

## Benchmark
```
Generating 100000 random sets with max-size 20
Generating similar data structure (error: 0.2)
"Elapsed time: 7305.048311 msecs"
Testing speed of nearest neighbor retrieval
Evaluation count : 2100 in 60 samples of 35 calls.
             Execution time mean : 29.915691 ms
    Execution time std-deviation : 3.117491 ms
   Execution time lower quantile : 24.151212 ms ( 2.5%)
   Execution time upper quantile : 35.281039 ms (97.5%)
                   Overhead used : 9.188202 ns

Found 1 outliers in 60 samples (1.6667 %)
	low-severe	 1 (1.6667 %)
 Variance from outliers : 72.0437 % Variance is severely inflated by outliers
Sample output
in #{T K f e x H X l} out #{T K f e q x P V O l u}
in #{s e Q L j v Y H R F i k g l u m} out #{T n z s Q L G S Y H F U i r g o}
in #{q R V} out #{q H R}
in #{z w q Z B a r X l u} out #{z w M H E R t i b r X l m D}
in #{d w q x M H F O k r g X u} out #{d n f q L x v E R F k A W D}
in #{T n e M B P t i r y g X N h m D c} out #{T z L J Y R F B P t y g X u A}
in #{d p j G J R u I} out #{d p j B r N u o}
in #{T f Q p j Y E D} out #{T z f p R U g m W D}
in #{z f Q Y F i N u h} out #{n s j Y F N u h o}
in #{n K Q v M H F B a V U O r y g u I c} out #{n K z s M Y E F B t O k y u I o c}
```
Benchmarks were run on OS X El Capitan (Intel Xeon E3-1240V2, 32 GB DDR3 RAM)

## Dependencies

* [clj-kdtree](https://github.com/abscondment/clj-kdtree)
* [java-lsh](https://github.com/tdebatty/java-LSH)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
