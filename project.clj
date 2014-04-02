(defproject om-todo "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2197"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [om "0.5.3"]
                 [org.clojars.franks42/cljs-uuid-utils "0.1.3"]
                 [org.clojure/clojure-contrib "1.2.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {
               :builds [{:id "dev"
                         :source-paths ["src"]
                         :compiler {
                                     :output-to "main.js"
                                     :output-dir "out"
                                     :optimizations :none
                                     :source-map true}}
                        {:id "release"
                         :source-paths ["src"]
                         :compiler {
                                     :output-to "main.js"
                                     :optimizations :advanced
                                     :pretty-print false
                                     :preamble ["react/react.min.js"]
                                     :externs ["react/externs/react.js"]}}]})
