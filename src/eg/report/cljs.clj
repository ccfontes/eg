(ns eg.report.cljs ^{:author "Carlos da Cunha Fontes"
                    :license {:name "The MIT License"
                              :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [cljs.test]
            [eg.report :refer [do-default-report
                               do-pred-report
                               do-example-pred-report
                               do-spec-report
                               do-expected-spec-report]]))

  (defmethod cljs.test/assert-expr 'eg.platform/valid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr true))
  
  (defmethod cljs.test/assert-expr 'eg.platform/invalid-spec?
    [_ _ assert-expr] (do-spec-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/equal?
    [_ _ assert-expr] (do-default-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/equal-ex?
    [_ _ assert-expr] (do-default-report assert-expr true))
    
  (defmethod cljs.test/assert-expr 'eg.platform/fn-identity-intercept
    [_ _ assert-expr] (do-pred-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/valid-expected-spec?
    [_ _ assert-expr] (do-expected-spec-report assert-expr false))
  
  (defmethod cljs.test/assert-expr 'eg.platform/pred-ex
    [_ _ assert-expr] (do-example-pred-report assert-expr true))
