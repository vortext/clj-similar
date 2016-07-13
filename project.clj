(defproject clj-similar "0.1.2"
  :description "Fast similar set lookup using MinHash and K-d trees"
  :url "https://github.com/vortext/clj-similar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:test {:dependencies [[criterium "0.4.4"]]}}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [info.debatty/java-lsh "0.9"]])
