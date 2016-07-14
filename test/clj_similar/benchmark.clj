(ns clj-similar.benchmark
  (:require [clojure.test :refer :all]
            [criterium.core :refer [quick-bench bench with-progress-reporting]]
            [clj-similar.core :refer :all]))


(def dict
  ;; Upper case + lower case ASCII letters
  (map (comp str char) (concat (range 65 91) (range 97 123))))

(defn random-set [max-size]
  (let [size (+ 1 (rand-int max-size))]
    (set (take size (repeatedly (rand-int 100) #_(rand-nth dict))))))

(defn generate-random
  [count max-size]
  (for [_ (range count)]
    (random-set max-size)))

(defn omit-random
  [s n]
  (let [omit (set (take n (shuffle s)))]
    (apply (partial disj s) omit)))

(deftest benchmark
  (let [count 1E5
        max-size 50
        coll (do
               (println "Generating" (long count) "random sets with max-size" max-size)
               (generate-random count max-size))
        s (do
            (println "Generating similar data structure")
            (time (similar coll 10 2)))]
    (println "Testing speed of nearest neighbor retrieval")
    #_(bench (nearest s (random-set max-size)))
    (println "Sample output for random target sets")
    (doseq [_ (range 10)]
      (let [in (random-set max-size)
            out1 (first (nearest s in 1 :exact? true))]
        (println "in" in "out" out1 "exact" (meta out1))))

    (println "Sample output for existing sets")
    (doseq [in (take 10 (random-sample 0.25 coll))]
      (let [part (omit-random in 5)
            out (nearest s part)]
        (println "in" part "original" in "out" out (meta out))))
    ))
