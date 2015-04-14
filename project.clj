(defproject fuzzy-urls "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [expectations "2.0.9"]]
  :main ^:skip-aot fuzzy-urls.example
  :target-path "target/%s"
  :plugins [[lein-expectations "0.0.7"]]
  :profiles {:uberjar {:aot :all}})
