# clj-similar

Experimental library for (fast) similar set lookup.
Under the hood it uses a [MinHash](https://en.wikipedia.org/wiki/MinHash) to compute a locality sensitive hashes of a collection of sets, and loads them into [k-d tree](https://en.wikipedia.org/wiki/K-d_tree).
The constructed similar set record can be used to retrieve [nearest neighbors](https://en.wikipedia.org/wiki/Nearest_neighbor_search) of a given point.

While the construction of the data structure is expensive, lookups should be fast.

Note that it will always return some set that is considered closest. Thresholding for a value that is "too dissimilar" is currently at your own discretion.


## Caveats
Collections of sets that have more than the maximum integer value of distinct values are currently unsupported.
The construction of the tree is expensive, due to Clojure's persistent datastructure overhead.
This is a *very* experimental library, it was literally written in 30 minutes, so don't expect much.

## Usage

```
(require '[clj-similar.core :refer [similar nearest]])
(def coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}])
(def s (similar coll))
(nearest s #{"f" "e" "a" "b"})
;=> {:value #{"f" "e" "a" "b"}, :dist-squared 0.0}
(nearest s #{"a" "b"} 2)
;=> ({:value #{"a" "b" "c"}, :dist-squared 8.0} {:value #{"f" "e" "a" "b"}, :dist-squared 12.0})
```

## Dependencies

* [clj-kdtree](https://github.com/abscondment/clj-kdtree)
* [java-lsh](https://github.com/tdebatty/java-LSH)

## License

Copyright © 2016 Joël Kuiper

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
