(ns eg.report.clj ^{:author "Carlos da Cunha Fontes"
                    :license {:name "The MIT License"
                              :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [clojure.string :as str]
            [clojure.test :as clj.test]
            [eg.platform]
            [eg.report :refer [->testing-fn-repr
                               do-default-report
                               do-pred-report
                               do-spec-report
                               do-expected-spec-report
                               print-report
                               spec-because]]))

(defmethod clj.test/assert-expr 'eg.platform/valid-spec?
  [_ assert-expr] (do-spec-report assert-expr true))

(defmethod clj.test/assert-expr 'eg.platform/invalid-spec?
  [_ assert-expr] (do-spec-report assert-expr false))

(defmethod clj.test/assert-expr 'eg.platform/equal?
  [_ assert-expr] (do-default-report assert-expr false))

(defmethod clj.test/assert-expr 'eg.platform/equal-ex?
  [_ assert-expr] (do-default-report assert-expr true))

(defmethod clj.test/assert-expr 'eg.platform/fn-identity-intercept
  [_ assert-expr] (do-pred-report assert-expr false))

(defmethod clj.test/assert-expr 'eg.platform/valid-expected-spec?
  [_ assert-expr] (do-expected-spec-report assert-expr false))

(defmethod clj.test/report :fail-spec
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [{:keys [spec-kw example example-code spec-error-data expect-valid? file line]}]
  (clj.test/with-test-out
    (let [example-code? (or (not= example example-code) (not expect-valid?))]
      (clj.test/inc-report-counter :fail)
      (println "\nFAIL in spec" (list spec-kw) (list (str file ":" line)))
      (if example-code? (println (str "  in example:" (if-not expect-valid? " !")) (pr-str example-code)))
      (println (spec-because example spec-error-data expect-valid?)))))

(defmethod clj.test/report :fail-default
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [m] (clj.test/with-test-out
        (clj.test/inc-report-counter :fail)
        (print-report m)))
