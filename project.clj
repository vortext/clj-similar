(defproject clj-similar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-kdtree "1.2.0" :exclusions [org.clojure/clojure]]
                 [info.debatty/java-lsh "0.9"]
                 ])
