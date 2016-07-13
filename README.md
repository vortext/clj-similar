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

## Dependencies

* [clj-kdtree](https://github.com/abscondment/clj-kdtree)
* [java-lsh](https://github.com/tdebatty/java-LSH)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
