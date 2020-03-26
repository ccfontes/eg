(ns eg.report.clj
  (:require [clojure.string :as str]
            [clojure.test :as clj.test]
            [eg.platform]
            [eg.report :refer [->testing-fn-repr do-equal-report]]))

(defmethod clojure.test/assert-expr 'eg.platform/equal?
  [_ form] (apply do-equal-report form))

(defmethod clj.test/report :fail-equal
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [{:keys [params expected actual] :as m}]
  (clj.test/with-test-out
    (clj.test/inc-report-counter :fail)
    (apply println "\nFAIL in function" (->testing-fn-repr m))
    (println "      params: " params)
    (println "    expected: " expected)
    (println "      actual: " actual)))
