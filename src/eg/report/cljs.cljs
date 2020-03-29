(ns eg.report.cljs
  (:require [clojure.string :as str]
            [cljs.test]
            [eg.platform]
            [eg.report :refer [->file-and-line-repr ->testing-fn-repr do-spec-report do-equal-report]]))

(when (exists? js/cljs.test$macros)
  ; defmethods for cljs JS
  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/valid-spec?
    [_ _ form] (do-spec-report form true))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/invalid-spec?
    [_ _ form] (do-spec-report form false))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/equal?
    [_ _ form] (apply do-equal-report (concat form [false])))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/equal-ex?
    [_ _ form] (apply do-equal-report (concat form [true]))))

(defmethod cljs.test/report [:cljs.test/default :fail-spec]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [spec-kw example example-code reason expect-valid? file line]}]
  (let [example-code? (or (not= example example-code) (not expect-valid?))
        file-and-line (->file-and-line-repr file line)]
    (cljs.test/inc-report-counter! :fail)
    (println "\nFAIL in spec" (list spec-kw) file-and-line)
    (if example-code? (println "in example:" (if expect-valid? "" "!") example-code))
    (println (str (if example-code? "   ") "because:")
             (if expect-valid?
               (if (exists? js/cljs.test$macros)
                 reason ; TODO improve error msg for same quality as in clj, and cljs JVM
                 (->> (str/split reason #" spec: ") butlast (str/join " spec: ")))
               (str example " - is a valid example")))))

(defmethod cljs.test/report [:cljs.test/default :fail-equal]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [params expected actual expression?] :as m}]
  (cljs.test/inc-report-counter! :fail)
  (if expression?
    (println "\nFAIL in expression at" (->testing-fn-repr m))
    (do (apply println "\nFAIL in function" (->testing-fn-repr m))
        (println "      params: " params)))
  (println "    expected: " expected)
  (println "      actual: " actual))
