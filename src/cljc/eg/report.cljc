(ns eg.report
  (:require [clojure.spec.alpha :as spec]
            [cljs.test :include-macros true]
            [clojure.test :as clj.test]
            [eg.platform :refer [->clj do-report]]))

(defn explain-str
  [& args] (apply spec/explain-str args))

(defn do-spec-report
  "Support customised spec examples in a test result and call report."
  [[f spec-kw example] expect-valid?]
  `(let [result# (~f ~spec-kw ~example)]
    (if result#
      (do-report {:type :pass})
      (do-report {:type          :fail-spec
                  :spec-kw       ~spec-kw
                  :example       ~example
                  :example-code  '~example
                  :expect-valid? ~expect-valid?
                  :reason        (explain-str ~spec-kw ~example)}))
    result#))

(defn do-equal-report
  "Support customised spec examples in a test result and call report."
  [equal expected [f & params :as actual]]
  `(let [result# (~equal (->clj ~expected) (->clj ~actual))]
    (if result#
      (do-report {:type :pass})
      (do-report {:type     :fail-equal
                  :function '~f
                  :params   (vec '~params)
                  :expected ~expected
                  :actual   ~actual}))
    result#))

#?(:clj
  (do
    ; defmethods for clj
    (defmethod clj.test/assert-expr 'eg.platform/valid-spec?
      [_ form] (do-spec-report form true))

    (defmethod clj.test/assert-expr 'eg.platform/invalid-spec?
      [_ form] (do-spec-report form false))

    ; defmethods for cljs JVM
    (defmethod cljs.test/assert-expr 'eg.platform/valid-spec?
      [_ _ form] (do-spec-report form true))

    (defmethod cljs.test/assert-expr 'eg.platform/invalid-spec?
      [_ _ form] (do-spec-report form false))

    (defmethod cljs.test/assert-expr 'eg.platform/equal?
      [_ _ form] (apply do-equal-report form)))

  :cljs
   (when (exists? js/cljs.test$macros)
     ; defmethods for cljs JS
     (defmethod js/cljs.test$macros.assert_expr 'eg.platform/valid-spec?
       [_ _ form] (do-spec-report form true))

     (defmethod js/cljs.test$macros.assert_expr 'eg.platform/invalid-spec?
       [_ _ form] (do-spec-report form false))

     (defmethod js/cljs.test$macros.assert_expr 'eg.platform/equal?
       [_ _ form] (apply do-equal-report form))))
