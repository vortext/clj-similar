(ns clj-similar.benchmark
  (:require [clojure.test :refer :all]
            [criterium.core :refer [quick-bench bench with-progress-reporting]]
            [clj-similar.core :refer :all]))


(def dict (map char (range 33 127)))

(defn random-set [max-size]
  (let [size (+ 1 (rand-int max-size))]
    (set (take size (repeatedly #(str (rand-nth dict)))))))

(defn generate-random
  [count max-size]
  (for [_ (range count)]
    (random-set max-size)))

(deftest benchmark
  (let [count 1E5
        max-size 50
        coll (do
               (println (str "Generating " (long count) " random sets with max-size " max-size))
               (generate-random count max-size))
        s (do
            (println "Generating similar data structure")
            (time (similar coll 0.1)))]
    (println "Testing speed of nearest neighbor retrieval")
    #_(bench (nearest s (random-set max-size)))
    (println "Sample output")
    (doseq [_ (range 10)]
      (println (nearest s (random-set max-size))))))
