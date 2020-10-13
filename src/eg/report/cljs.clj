(ns eg.report.cljs ^{:author "Carlos da Cunha Fontes"
                    :license {:name "The MIT License"
                              :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [cljs.test]
            [eg.report :refer [do-example-equal-report
                               do-example-pred-report
                               do-expression-equal-report
                               do-expression-pred-report
                               do-expected-spec-report
                               do-spec-report]]))

  (defmethod cljs.test/assert-expr 'eg.platform/valid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr true))
  
  (defmethod cljs.test/assert-expr 'eg.platform/invalid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/equal?
    [_ _ assert-expr] (do-example-equal-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/equal-ex?
    [_ _ assert-expr] (do-expression-equal-report assert-expr true))
    
  (defmethod cljs.test/assert-expr 'eg.platform/fn-identity-intercept
    [_ _ assert-expr] (do-example-pred-report assert-expr))
  
  (defmethod cljs.test/assert-expr 'eg.platform/valid-expected-spec?
    [_ _ assert-expr] (do-expected-spec-report assert-expr))
  
  (defmethod cljs.test/assert-expr 'eg.platform/pred-ex
    [_ _ assert-expr] (do-expression-pred-report assert-expr))
