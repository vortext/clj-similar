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
        s (similar coll 10 5)]
    (testing "Return the nearest set when exact match"
      (is (all-in? (nearest s #{"f" "e" "a" "b"}) #{"f" "e" "a" "b"})))
    (testing "Return nil if element is unseen"
      (is (= (nearest s #{"x"}) nil)))
    (testing "Return the nearest set when fuzzy match with extra element"
      (is (all-in? (nearest s #{"f" "e" "a" "b" "x"}) #{"f" "e" "a" "b"})))
    (testing "Return the nearest set when fuzzy match with omitted element"
      (is (all-in? (nearest s #{"a"}) #{"a" "b" "c"})))
    (testing "Return the nearest two sets"
      (is (all-in? (nearest s #{"a" "b"} 2) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))))

(deftest threshold-test
  (let [coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}]
        s (similar coll 10 5)]
    (testing "omit too values with a too low jaccard-index"
      (is (all-in? (nearest s #{"x"} 1 :threshold 0.8) '())))
    (testing "omit too values with a too low jaccard-index"
      (is (all-in? (nearest s #{"a" "b"} 3 :threshold 0.4) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))
    ))
