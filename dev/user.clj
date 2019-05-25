(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.test]
            [eg :refer [eg ge ex]]
            [eg.test.pass]))

(defn run-tests []
  (refresh)
  (clojure.test/run-tests 'eg.test.pass 'user))
