(ns clj-similar.core-test
  (:require [clojure.test :refer :all]
            [clj-similar.core :refer :all]))

(defn in?
  "true if coll contains elm"
  [coll el]
  (some #(= el %) coll))

(defn all-in?
  [coll els]
  (every? (partial in? coll) els))

(deftest simple-test
  (let [coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}]
        s (similar coll 0.01)]
    (testing "Return the nearest set when exact match"
      (is (all-in? (nearest s #{"f" "e" "a" "b"}) #{"f" "e" "a" "b"})))
    (testing "Return the nearest set when fuzzy match with extra element"
      (is (all-in? (nearest s #{"f" "e" "a" "b" "x"}) #{"f" "e" "a" "b"})))
    (testing "Return the nearest set when fuzzy match with omitted element"
      (is (all-in? (nearest s #{"a"}) #{"a" "b" "c"})))
    (testing "Return the nearest two sets"
      (is (all-in? (nearest s #{"a" "b"} 2) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))
    ))

(deftest threshold-test
  (let [coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}]
        s (similar coll 0.01)]
    (testing "omit too values with a too low jaccard-index (exact? true)"
      (is (all-in? (nearest s #{"x"} 1 :threshold 0.8 :exact? true) '())))
    (testing "omit too values with a too low jaccard-index (exact? true)"
      (is (all-in? (nearest s #{"a" "b"} 2 :threshold 0.4 :exact? true) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))
    (testing "omit too values with a too low jaccard-index (exact? false)"
      (is (all-in? (nearest s #{"x"} 1 :threshold 0.8 :exact? false) '())))
    (testing "omit too values with a too low jaccard-index (exact? false)"
      (is (all-in? (nearest s #{"a" "b"} 2 :threshold 0.4 :exact? false) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))))
