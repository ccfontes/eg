(defproject eg "0.1.0-alpha"
  :description "A maximal reduction of noise for tests using clojure.test"
  :license {:name "The Universal Permissive License (UPL), Version 1.0"
            :url "https://opensource.org/licenses/UPL"}
  :url "https://github.com/ccfontes/eg"
  :scm {:name "git"
        :url "https://github.com/ccfontes/eg"}
  :jar-name "eg.jar"
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :source-paths ["src/cljc"]
  :test-paths ["test/pass/cljc" "test/fail/cljc"]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.0"]
                                  [org.clojure/clojurescript "1.10.520" :scope "provided"]]}}
  :aliases {"test-pass" ["do" ["test" "eg.test.pass"] ["cljsbuild" "test" "pass-node"]]
            "test-fail" ["do" ["test" "eg.test.fail"] ["cljsbuild" "test" "fail-node"]]}
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {:test-commands {"pass-node" ["node" "target/out/test/pass/runner.js"]
                              "fail-node" ["node" "target/out/test/fail/runner.js"]}
              :builds {:pass-test
                        {:source-paths   ["src/cljc" "test/pass/cljc" "test/pass/cljs"]
                         :compiler       {:target         :nodejs
                                          :main           eg.test.pass.runner
                                          :output-to      "target/out/test/pass/runner.js"
                                          :output-dir     "target/out/test/pass"
                                          :optimizations  :none}}
                       :fail-test
                         {:source-paths   ["src/cljc" "test/fail/cljc" "test/fail/cljs"]
                          :compiler       {:target         :nodejs
                                           :main           eg.test.fail.runner
                                           :output-to      "target/out/test/fail/runner.js"
                                           :output-dir     "target/out/test/fail"
                                           :optimizations  :none}}}})

