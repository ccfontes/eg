(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.test :refer [deftest is]]
            [eg :refer [eg ge ex]]
            [eg.test.pass]))

(defn run-tests []
  (refresh)
  (clojure.test/run-tests 'eg.test.pass 'user))

(intern 'eg 'refresh refresh)
(intern 'eg.platform 'refresh refresh)
(intern 'eg 'run-tests run-tests)
(intern 'eg.platform 'run-tests run-tests)
