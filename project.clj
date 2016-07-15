(defproject clj-similar "0.1.4"
  :description "Fast similar set lookup using MinHash and K-d trees"
  :url "https://github.com/vortext/clj-similar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:test {:dependencies [[criterium "0.4.4"]]}}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [info.debatty/java-lsh "0.9"]])
