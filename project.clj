(defproject eg "0.5.5-alpha"
  :description "eg delivers clojure.test function tests with conciseness."
  :license {:name "The MIT License"
            :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}
  :url "https://github.com/ccfontes/eg"
  :scm {:name "git"
        :url "https://github.com/ccfontes/eg"}
  :jar-name "eg.jar"
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :source-paths ["src"]
  :test-paths ["test/pass" "test/fail"]
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]]
  :aliases {"clj-test-pass-unit"  ["test" "eg.test.pass.unit"]
            "cljs-test-pass-unit" ["with-profiles" "+cljs-test-pass-unit" "cljsbuild" "test"]
            "clj-test-pass-integration"  ["test" "eg.test.pass.integration"]
            "cljs-test-pass-integration" ["with-profiles" "+cljs-test-pass-integration" "cljsbuild" "test"]
            "clj-test-fail"  ["test" "eg.test.fail"]
            "cljs-test-fail" ["with-profiles" "+cljs-test-fail" "cljsbuild" "test"]
            "coverage"       ["with-profiles" "+cloverage" "cloverage" "--codecov"]}
  :plugins [[lein-cljsbuild "1.1.7"] [lein-tach "1.0.0"]]
  :dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]
                 [org.clojure/clojure "1.10.0" :scope "provided"]
                 ; clojurescript not provided, otherwise Clojure programs would
                 ; need to include clojurescript in their dependencies in order
                 ; to not break 'eg.report.cljs.clj'
                 [org.clojure/clojurescript "1.10.520"]]
  :repl-options {:init (clojure.tools.namespace.repl/refresh)
                 :welcome (do (println "To refresh all namespaces, run: (refresh)")
                              (println "To run all tests, run: (run-tests)"))}
  :tach {:test-runner-ns eg.test.pass.runner
         :source-paths ["src" "test/pass"]
         :force-non-zero-exit-on-test-failure? true}
  :profiles
    {:repl {:source-paths ["repl"]}
     :dev {:dependencies [[pjstadig/humane-test-output "0.9.0"]]
           :injections [(require 'pjstadig.humane-test-output)
                        (pjstadig.humane-test-output/activate!)]}
     :cloverage {:plugins [[lein-cloverage "1.1.1"]]
                 :cloverage {:test-ns-regex [#"^eg\.test\.pass\.unit$"
                                             #"^eg\.test\.pass\.integration$"]}}
     :cljs-test-pass-unit
       {:cljsbuild
         {:test-commands {"pass-unit-node" ["node" "target/out/test/pass/unit/runner.js"]}
          :builds
           {:test
             {:source-paths ["src" "test/pass"]
              :compiler     {:target        :nodejs
                             :main          eg.test.pass.unit.runner
                             :output-to     "target/out/test/pass/unit/runner.js"
                             :output-dir    "target/out/test/pass/unit"
                             :optimizations :none
                             :source-map    true
                             :warnings      {:single-segment-namespace false}}}}}}
     :cljs-test-pass-integration
       {:cljsbuild
         {:test-commands {"pass-integration-node" ["node" "target/out/test/pass/integration/runner.js"]}
           :builds
            {:test
              {:source-paths ["src" "test/pass"]
               :compiler     {:target        :nodejs
                              :main          eg.test.pass.integration.runner
                              :output-to     "target/out/test/pass/integration/runner.js"
                              :output-dir    "target/out/test/pass/integration"
                              :optimizations :none
                              :source-map    true
                              :warnings      {:single-segment-namespace false}}}}}}
     :cljs-test-fail
      {:cljsbuild
        {:test-commands {"fail-node" ["node" "target/out/test/fail/runner.js"]}
         :builds
          {:test
            {:source-paths ["src" "test/fail"]
             :compiler     {:target        :nodejs
                            :main          eg.test.fail.runner
                            :output-to     "target/out/test/fail/runner.js"
                            :output-dir    "target/out/test/fail"
                            :optimizations :none
                            :warnings      {:single-segment-namespace false}}}}}}})
