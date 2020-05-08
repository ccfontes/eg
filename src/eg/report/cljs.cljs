(ns eg.report.cljs ^{:author "Carlos da Cunha Fontes"
                     :license {:name "The MIT License"
                               :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [clojure.string :as str]
            [cljs.test]
            [eg.platform]
            [eg.report :refer [->file-and-line-repr
                               ->testing-fn-repr
                               do-equal-report
                               do-fn-report
                               do-spec-report
                               print-report]]))

(when (exists? js/cljs.test$macros)
  ; defmethods for cljs JS
  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/valid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr true))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/invalid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr false))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/equal?
    [_ _ assert-expr] (do-equal-report assert-expr false))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/equal-ex?
    [_ _ assert-expr] (do-equal-report assert-expr true))

  (defmethod js/cljs.test$macros.assert_expr 'eg.platform/fn-identity-intercept
    [_ _ assert-expr] (do-fn-report assert-expr true)))

(defmethod cljs.test/report [:cljs.test/default :fail-spec]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [spec-kw example example-code reason expect-valid? file line]}]
  (let [example-code? (or (not= example example-code) (not expect-valid?))
        file-and-line (->file-and-line-repr file line)]
    (cljs.test/inc-report-counter! :fail)
    (println "\nFAIL in spec" (list spec-kw) file-and-line)
    (if example-code? (println (str "  in example:" (if-not expect-valid? "!")) example-code))
    (println "     because:"
             (if expect-valid?
               (if (exists? js/cljs.test$macros)
                 reason ; TODO improve error msg for same quality as in clj, and cljs JVM
                 (->> (str/split reason #" spec: ") butlast (str/join " spec: ")))
               (str example " - is a valid example")))))

(defmethod cljs.test/report [:cljs.test/default :fail-equal]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [m] (cljs.test/inc-report-counter! :fail)
      (print-report m))
