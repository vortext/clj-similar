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
(def s (similar coll)) ;; default similarity estimation error (0.1)
(def s (similar coll 0.01)) ;; with a given similarity estimation error.

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

;; By default this uses the exact Jaccard Index
;; You can calculate the approximate jaccard indexes instead by passing exact? false
(nearest s #{"a" "b"} 2 :threshold 0.6 :exact? false)

;; The values of the sets can be any Clojure data structure, even other collections
(def coll [#{["a"] ["a" "b"]} #{["c" "d"] ["a" "c"]}])
(def s (similar coll))
(nearest s #{["a" "b"]})
;=> #{["a" "b"] ["a"]}

```

## Benchmark
```
Generating 100000 random sets with max-size 20
Generating similar data structure (error: 0.1)
"Elapsed time: 10559.197109 msecs"
Testing speed of nearest neighbor retrieval
Evaluation count : 780 in 60 samples of 13 calls.
             Execution time mean : 90.004008 ms
    Execution time std-deviation : 8.606392 ms
   Execution time lower quantile : 70.950717 ms ( 2.5%)
   Execution time upper quantile : 102.682276 ms (97.5%)
                   Overhead used : 9.193921 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 67.0034 % Variance is severely inflated by outliers
```
The most determining factor is the target error rate, which generates more collisions but is faster.
When a collision occurs the elements are stored in the same bucket, and sorted by their Jaccard Index (highest first).
```
Generating 100000 random sets with max-size 20
Generating similar data structure (error: 0.2)
"Elapsed time: 8567.275593 msecs"
Testing speed of nearest neighbor retrieval
^[Evaluation count : 6420 in 60 samples of 107 calls.
             Execution time mean : 9.390420 ms
    Execution time std-deviation : 573.624010 µs
   Execution time lower quantile : 8.161624 ms ( 2.5%)
   Execution time upper quantile : 10.564736 ms (97.5%)
                   Overhead used : 9.220449 ns

Found 5 outliers in 60 samples (8.3333 %)
	low-severe	 3 (5.0000 %)
	low-mild	 2 (3.3333 %)
 Variance from outliers : 45.1468 % Variance is moderately inflated by outliers
```

```
Generating 100000 random sets with max-size 25
Generating similar data structure (error: 0.25)
"Elapsed time: 9428.04388 msecs"
Testing speed of nearest neighbor retrieval
Evaluation count : 19320 in 60 samples of 322 calls.
             Execution time mean : 3.285121 ms
    Execution time std-deviation : 135.236058 µs
   Execution time lower quantile : 3.078034 ms ( 2.5%)
   Execution time upper quantile : 3.555667 ms (97.5%)
                   Overhead used : 9.226428 ns
Sample output for random target sets
```

Benchmarks were run on OS X El Capitan (Intel Xeon E3-1240V2 @ 3.4Ghz, 32 GB DDR3 RAM)

## Dependencies

* [java-lsh](https://github.com/tdebatty/java-LSH)
* [K-d tree by Levy](http://home.wlu.edu/~levys/software/kd/)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
