(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.test :as test :refer [deftest is]]
            [eg :refer [eg ge ex]]
            [eg.test.pass.unit]
            [eg.test.pass.integration]))

(defn run-tests []
  (refresh)
  (test/run-tests 'eg.test.pass.unit 'eg.test.pass.integration 'user))

(intern 'eg 'refresh refresh)
(intern 'eg.platform 'refresh refresh)
(intern 'eg 'run-tests run-tests)
(intern 'eg.platform 'run-tests run-tests)
