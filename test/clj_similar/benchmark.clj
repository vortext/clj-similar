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

(defn omit-random
  [s n]
  (let [omit (set (take n (shuffle s)))]
    (apply (partial disj s) omit)))

(deftest benchmark
  (let [count 1E6
        max-size 10
        coll (do
               (println "Generating" (long count) "random sets with max-size" max-size)
               (generate-random count max-size))
        s (do
            (println "Generating similar data structure")
            (time (similar coll 10 3)))]
    (println "Testing speed of nearest neighbor retrieval")
    (println "Sample output for random target sets")
    (doseq [_ (range 10)]
      (let [in (random-set max-size)
            out (nearest s in 2)]
        (println "in" in "out" out "approximate" (map meta out))))

    (println "Sample output for existing sets")
    (doseq [in (take 10 (random-sample 0.25 coll))]
      (let [part (omit-random in 2)
            out (nearest s part 2 :exact? true)]
        (println "in" part "original" in "out" out "exact" (map meta out))))
    (bench (nearest s (random-set max-size)))
    ))
