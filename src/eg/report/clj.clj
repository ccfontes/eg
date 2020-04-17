(ns eg.report.clj
  (:require [clojure.string :as str]
            [clojure.test :as clj.test]
            [eg.platform]
            [eg.report :refer [->testing-fn-repr do-equal-report do-spec-report print-report]]))

(defmethod clj.test/assert-expr 'eg.platform/valid-spec?
  [_ form] (do-spec-report form true))

(defmethod clj.test/assert-expr 'eg.platform/invalid-spec?
  [_ form] (do-spec-report form false))

(defmethod clj.test/assert-expr 'eg.platform/equal?
  [_ form] (apply do-equal-report (concat form [false])))

(defmethod clj.test/assert-expr 'eg.platform/equal-ex?
  [_ form] (apply do-equal-report (concat form [true])))

(defmethod clj.test/report :fail-spec
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [{:keys [spec-kw example example-code reason expect-valid? file line]}]
  (clj.test/with-test-out
    (let [example-code? (or (not= example example-code) (not expect-valid?))]
      (clj.test/inc-report-counter :fail)
      (println "\nFAIL in spec" (list spec-kw) (list (str file ":" line)))
      (if example-code? (println "in example:" (if expect-valid? "" "!") (pr-str example-code)))
      (println (str (if example-code? "   ") "because:")
               (if expect-valid?
                 (->> (str/split reason #" spec: ") butlast (str/join " spec: "))
                 (str example " - is a valid example"))))))

(defmethod clj.test/report :fail-equal
  ; Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj
  [m] (clj.test/with-test-out
        (clj.test/inc-report-counter :fail)
        (print-report m)))
