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
  (let [count 1000
        max-size 25
        similarity-error 0.1
        coll (do
               (println "Generating" (long count) "random sets with max-size" max-size)
               (generate-random count max-size))
        s (do
            (println "Generating similar data structure" (str "(error: " similarity-error ")"))
            (time (similar coll similarity-error)))]
    (println "Testing speed of nearest neighbor retrieval")
    #_(bench (nearest s (random-set max-size)))
    (println "Sample output for random target sets")
    (doseq [_ (range 10)]
      (let [in (random-set max-size)
            out1 (first (nearest s in 1 :exact? true))
            out2 (first (nearest s in 1 :exact? false))]
        (println "exact" "in" in "out" out1 "exact" (meta out1) "approx" (meta out2))))
    ))
