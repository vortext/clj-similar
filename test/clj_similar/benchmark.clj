(ns clj-similar.benchmark
  (:require [clojure.test :refer :all]
            [criterium.core :refer [quick-bench bench with-progress-reporting]]
            [clj-similar.core :refer :all]))


(def dict
  ;; Upper case + lower case ASCII letters
  (map (comp str char) (concat (range 65 91) (range 97 123))))

(defn random-set [max-size]
  (let [size (+ 1 (rand-int max-size))]
    (set (take size (repeatedly #(rand-nth dict))))))

(defn generate-random
  [count max-size]
  (for [_ (range count)]
    (random-set max-size)))

(deftest benchmark
  (let [count 1000
        max-size 10
        similarity-error 0.2
        coll (do
               (println "Generating" (long count) "random sets with max-size" max-size)
               (generate-random count max-size))
        s (do
            (println "Generating similar data structure" (str "(error: " similarity-error ")"))
            (time (similar coll similarity-error)))]
    (println "Testing speed of nearest neighbor retrieval")
    (bench (nearest s (random-set max-size)))
    (println "Sample output")
    (doseq [_ (range 10)]
      (let [in (random-set max-size)
            out (first (nearest s in 1 :exact? true))]
        (println "in" in "out" out (meta out))))))
