(ns eg.report.clj ^{:author "Carlos da Cunha Fontes"
                    :license {:name "The MIT License"
                              :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [clojure.string :as str]
            [clojure.test :as clj.test]
            [eg.platform]
            [eg.report :refer [->testing-fn-repr
                               do-example-equal-report
                               do-example-pred-report
                               do-expression-equal-report
                               do-expression-pred-report
                               do-spec-report
                               do-expected-spec-report
                               do-expression-expected-spec-report
                               print-report
                               spec->because-error
                               spec-because]]))

(defmethod clj.test/assert-expr 'eg.platform/valid-spec?
  [_ assert-expr] (do-spec-report assert-expr true))

(defmethod clj.test/assert-expr 'eg.platform/invalid-spec?
  [_ assert-expr] (do-spec-report assert-expr false))

(defmethod clj.test/assert-expr 'eg.platform/equal-eg?
  [_ assert-expr] (do-example-equal-report assert-expr))

(defmethod clj.test/assert-expr 'eg.platform/equal-ex?
  [_ assert-expr] (do-expression-equal-report assert-expr))

(defmethod clj.test/assert-expr 'eg.platform/pred-eg
  [_ assert-expr] (do-example-pred-report assert-expr))

(defmethod clj.test/assert-expr 'eg.platform/pred-ex
  [_ assert-expr] (do-expression-pred-report assert-expr))

(defmethod clj.test/assert-expr 'eg.platform/valid-expected-spec?
  [_ assert-expr] (do-expected-spec-report assert-expr))

(defmethod clj.test/assert-expr 'eg.platform/valid-expected-spec-ex?
  [_ assert-expr] (do-expression-expected-spec-report assert-expr))

(defmethod clj.test/report :fail-spec
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [{:keys [spec-kw example example-code spec-error-data expect-valid? file line]}]
  (clj.test/with-test-out
    (let [example-code? (or (not= example example-code) (not expect-valid?))]
      (clj.test/inc-report-counter :fail)
      (println "\nFAIL in spec" (list spec-kw) (list (str file ":" line)))
      (if example-code? (println (str "in example:" (if-not expect-valid? " !")) (pr-str example-code)))
      (println (spec-because example (some-> spec-error-data spec->because-error) expect-valid?)))))

(defmethod clj.test/report :fail-default
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [m] (clj.test/with-test-out
        (clj.test/inc-report-counter :fail)
        (print-report m)))
