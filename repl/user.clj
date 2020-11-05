(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.test :as test :refer [deftest is]]
            [clojure.spec.alpha :as spec]
            [eg :as eg :refer [eg ge ex]]
            [eg.spec :as eg-spec]
            [eg.test.fixtures :as fixtures]
            [eg.test.pass.unit]
            [eg.test.pass.integration]))

(defn run-tests []
  (refresh)
  (test/run-tests 'eg.test.pass.unit 'eg.test.pass.integration 'user))

(intern 'eg 'refresh refresh)
(intern 'eg.platform 'refresh refresh)
(intern 'eg 'run-tests run-tests)
(intern 'eg.platform 'run-tests run-tests)
