(defproject clj-similar "0.1.1"
  :description "Fast similar set lookup using MinHash and K-d trees"
  :url "https://github.com/vortext/clj-similar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-kdtree "1.2.0" :exclusions [org.clojure/clojure]]
                 [info.debatty/java-lsh "0.9"]])
