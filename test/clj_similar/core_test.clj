(ns clj-similar.core-test
  (:require [clojure.test :refer :all]
            [clj-similar.core :refer :all]))

(deftest simple-test
  (let [coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}]
        s (similar coll)]
    (testing "Return the nearest set when exact match"
      (is (= (nearest s #{"f" "e" "a" "b"}) #{"f" "e" "a" "b"})))
    (testing "Return the nearest set when fuzzy match with extra element"
      (is (= (nearest s #{"f" "e" "a" "b" "x"}) #{"f" "e" "a" "b"})))
    (testing "Return the nearest set when fuzzy match with omitted element"
      (is (= (nearest s #{"a"}) #{"a" "b" "c"})))
    (testing "Return the nearest two sets"
      (is (= (nearest s #{"a" "b"} 2) '(#{"a" "b" "c"} #{"f" "e" "a" "b"}))))
    ))

(deftest threshold-test
  (let [coll [#{"a" "b" "c"} #{"d" "e" "c"} #{"f" "e" "a" "b"}]
        s (similar coll)]
    (testing "Omit too values with a too low jaccard-index"
      (is (= (nearest s #{"x"} 1 :threshold 0.5) '())))
    (testing "Omit too values with a too low jaccard-index"
      (is (= (nearest s #{"a" "b"} 2 :threshold 0.6) '(#{"a" "b" "c"}))))))
