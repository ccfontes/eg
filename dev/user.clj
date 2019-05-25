(ns user
  (:require [clojure.tools.namespace.repl]
            [clojure.test :refer [deftest is]]
            [eg :refer [eg ge ex]]
            [eg.test.pass]))

(defn run-tests- []
  (clojure.tools.namespace.repl/refresh)
  (clojure.test/run-tests 'eg.test.pass 'user))

(intern 'clojure.core 'refresh clojure.tools.namespace.repl/refresh)

(intern 'clojure.core 'run-tests run-tests-)
