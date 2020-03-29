(defproject eg "0.4.15-alpha"
  :description "eg delivers clojure.test function tests with conciseness."
  :license {:name "The Universal Permissive License (UPL), Version 1.0"
            :url "https://opensource.org/licenses/UPL"}
  :url "https://github.com/ccfontes/eg"
  :scm {:name "git"
        :url "https://github.com/ccfontes/eg"}
  :jar-name "eg.jar"
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :source-paths ["src"]
  :test-paths ["test/pass" "test/fail"]
  :deploy-repositories [["releases" :clojars]]
  :aliases {"clj-test-pass"  ["test" "eg.test.pass"]
            "cljs-test-pass" ["with-profiles" "+cljs-test-pass" "cljsbuild" "test"]
            "clj-test-fail"  ["test" "eg.test.fail"]
            "cljs-test-fail" ["with-profiles" "+cljs-test-fail" "cljsbuild" "test"]
            "coverage"       ["with-profiles" "+cloverage" "cloverage" "--codecov"]}
  :plugins [[lein-cljsbuild "1.1.7"] [lein-tach "1.0.0"]]
  :dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]]
  :repl-options {:init (clojure.tools.namespace.repl/refresh)
                 :welcome (do (println "To refresh all namespaces, run: (refresh)")
                              (println "To run all tests, run: (run-tests)"))}
  :tach {:test-runner-ns eg.test.pass.runner
         :source-paths ["src" "test/pass"]
         :force-non-zero-exit-on-test-failure? true}
  :profiles
    {:repl {:source-paths ["repl"]}
     :dev {:dependencies [[org.clojure/clojure "1.10.0"]
                          [org.clojure/clojurescript "1.10.520" :scope "provided"]
                          [pjstadig/humane-test-output "0.9.0"]]
           :injections [(require 'pjstadig.humane-test-output)
                        (pjstadig.humane-test-output/activate!)]}
     :cloverage {:plugins [[lein-cloverage "1.1.1"]]
                 :cloverage {:test-ns-regex [#"^eg\.test\.pass$"]}}
     :cljs-test-pass
      {:cljsbuild
        {:test-commands {"pass-node" ["node" "target/out/test/pass/runner.js"]}
         :builds
          {:test
            {:source-paths ["src" "test/pass"]
             :compiler     {:target        :nodejs
                            :main          eg.test.pass.runner
                            :output-to     "target/out/test/pass/runner.js"
                            :output-dir    "target/out/test/pass"
                            :optimizations :none
                            :warnings {:single-segment-namespace false}}}}}}
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
                            :warnings {:single-segment-namespace false}}}}}}})
