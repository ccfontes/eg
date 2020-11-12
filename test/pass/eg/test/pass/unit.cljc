(ns eg.test.pass.unit
    (:require [eg.platform :as platform :refer [deftest is testing cross-throw]]
              [clojure.spec.alpha :as spec]
              [eg.test.fixtures :as fixtures]
              [eg.spec :as eg-spec]
              [eg :as eg :refer [ffilter
                                 assoc-if-new
                                 map-dregs
                                 prepare-assertion-expression
                                 test?
                                 assoc-focus-metas
                                 dont-care?
                                 named-dont-care?
                                 fill-dont-cares
                                 ensure-vec-wrapped-params
                                 prepare-examples
                                 ->test-name
                                 cljs-safe-namespace
                                 rm-lead-colon]]
              [eg.report :as report]))

(deftest ffilter-test
  (is (= nil (ffilter odd? nil)))
  (is (= nil (ffilter odd? [])))
  (is (= 4 (ffilter even? [4])))
  (is (= 1 (ffilter odd? [2 1 4]))))

(deftest assoc-if-new-test
  (is (= {:foo "bar"} (assoc-if-new nil :foo "bar")))
  (is (= {:foo "bar"} (assoc-if-new {} :foo "bar")))
  (is (= {:spam nil} (assoc-if-new {:spam nil} :spam "bar")))
  (is (= {:foo "bar" :spam "baz"} (assoc-if-new {:spam "baz"} :foo "bar"))))

(deftest map-dregs-test
  (is (= [] (map-dregs vector nil)))
  (is (= [] (map-dregs vector '())))
  (is (= [ [1] ] (map-dregs vector [1])))
  (is (= [ [1 1] [2] ] (map-dregs vector '(1) [] [1 2]))))

(deftest prepare-assertion-expression-test
  (is (= {:expression 4 :expected 'boolean} (prepare-assertion-expression [4])))
  (is (re-find #"failed"
               (try (prepare-assertion-expression [2 3 7])
                 (catch #?(:clj Exception :cljs :default) e
                   #?(:clj (-> e Throwable->map :cause))
                   #?(:cljs (.-message e)))))))

(deftest test?-test
  (is (test? (atom {:clojure.core/some false :clojure.core/any? nil})   true))
  (is (test? (atom {:clojure.core/some true  :clojure.core/any? nil})   true))
  (is (test? (atom {:clojure.core/some nil   :clojure.core/any? false}) nil))
  (is (not (test? (atom {:clojure.core/some false :clojure.core/any? true})  nil)))
  (is (not (test? (atom {:clojure.core/some true :clojure.core/any? true}) nil))))

(deftest rm-colon-test
  (is (= "foo" (rm-lead-colon ":foo")))
  (is (= "bar" (rm-lead-colon "bar"))))

(deftest cljs-safe-namespace-test
  (is (= "clojure-core" (cljs-safe-namespace 'clojure.core/inc)))
  (is (= "clojure-core" (cljs-safe-namespace :clojure.core/inc))))

(deftest ->test-name-test
  (is (symbol? (->test-name 'inc)))
  (is (symbol? (->test-name ::fixtures/int)))
  (is (= 'inc-test (->test-name 'inc)))
  (is (= 'clojure-core-inc-test (->test-name 'clojure.core/inc)))
  (is (= 'eg-test-fixtures-:int-test (->test-name ::fixtures/int))))

(deftest assoc-focus-metas-test
  (let [inc-meta (-> 'seq resolve meta)
        inc-w-focus-meta (assoc inc-meta :focus true)]
    (is (= {#?(:clj :clojure.core/seq :cljs :cljs.core/seq) true}
           (assoc-focus-metas {} inc-w-focus-meta 'seq)))
    (is (= {#?(:clj :clojure.core/seq :cljs :cljs.core/seq) nil}
           (assoc-focus-metas {} inc-meta 'seq)))))

(deftest fill-dont-cares-test
  (is (= [{:params [1 2] :expected :a}
          {:params [1 4] :operator nil :expected :b}
          {:params [5 6] :expected :c}]
         (fill-dont-cares [{:params [1 2] :expected :a}
                           {:params ['_ 4] :expected :b}
                           {:params [5 6] :expected :c}])))
  (is (= [{:params [1 2] :expected :a}
          {:params [1 4] :operator nil :expected :b}
          {:params [5] :expected :c}]
         (fill-dont-cares [{:params [1 2] :expected :a}
                           {:params ['_ 4] :expected :b}, {:params [5] :expected :c}])))
  (is (= [{:params [5 2] :expected :b}
          {:params [5 2] :operator nil :expected [{:a 5} 5]}]
         (fill-dont-cares [{:params [5 2] :expected :b}
                           {:params ['$1 2] :expected [{:a '$1} '$1]}])))
  (is (= [{:params [5 2] :operator '= :expected :b}
          {:params [5 2] :operator '=> :expected [{:a 5} 5]}]
         (fill-dont-cares [{:params [5 2] :operator '= :expected :b}
                           {:params ['$1 2] :operator '=> :expected [{:a '$1} '$1]}])))
  (is (= [{:params [1 2] :expected nil}
          {:params [1 4] :operator nil :expected :b}]
         (fill-dont-cares [{:params [1 2] :expected nil}
                           {:params ['_ 4] :expected :b}])))
  (is (= "No choices found for don't care: _"
         (try (doall (fill-dont-cares [{:params ['_ 4] :expected :b}]))
           (catch #?(:clj Exception :cljs :default) e
             #?(:clj (-> e Throwable->map :cause))
             #?(:cljs (.-message e)))))))

(deftest ensure-vec-wrapped-params-test
  (is {:params ['(4)]} (ensure-vec-wrapped-params {:params '(4)}))
  (is {:params [4]} (ensure-vec-wrapped-params {:params [4]})))

(deftest prepare-examples-test
  (is [{:params [3 2] :expected 5}
       {:params [3 1] :expected 4}
       {:params [4]}]
      (prepare-examples [:function
                         {:examples [[:implicit-straight-arrow {:params [3 2] :expected 5}]
                                     [:implicit-straight-arrow {:params ['_ 1] :expected 4}]
                                     [:implicit-straight-arrow {:params 4}]]}]))
  (is [{:spec-example 4}]
      (prepare-examples [:spec {:examples [[:spec-example 4]]}])))

; only Clojure can get metadata from a function using 'meta'
#?(:clj (defmacro macro-fn-meta-fixture [f] (meta f)))
#?(:clj (deftest macro-fn-meta-simulated-test
    (is (= {:focus true} (macro-fn-meta-fixture ^:focus inc)))))

(deftest named-dont-care-test
  (is (named-dont-care? '$thing))
  (is (not (named-dont-care? 'thing)))
  (is (not (named-dont-care? "thing"))))

(deftest dont-care-test
  (is (dont-care? '_))
  (is (dont-care? '$foo)))

(deftest cross-throw-test
  (is (= "BOOM" (try (cross-throw "BOOM")
                  (catch #?(:clj Exception :cljs :default) e
                    #?(:clj (-> e Throwable->map :cause))
                    #?(:cljs (.-message e)))))))

(deftest if-cljs-test
  #?(:cljs (do (is (= "cljs" (platform/if-target-is-cljs "cljs" "clj")))
               (is (= "cljs" (platform/if-target-is-cljs "cljs"))))
     :clj (do (is (= "clj" (platform/if-target-is-cljs "cljs" "clj")))
              (is (= nil (platform/if-target-is-cljs "clj"))))))

(deftest equal-eg?-test
  (is (true? (platform/equal-eg? 3 3)))
  (is (= (= 3 3) (platform/equal-eg? 3 3))))

(deftest equal-ex?-test
  (is (boolean? (platform/equal-ex? 3 3))))

(deftest pred-ex-test
  (is (= "truthy" (platform/pred-ex "truthy"))))

#?(:cljs
  (deftest rm-cljsjs-st-fname-prefix-fluff-test
    (is (= "eg/test/pass.js:2590:27"
           (report/rm-cljsjs-st-fname-prefix-fluff "cljs$lang$test@file:///eg/test/pass.js:2590:27")))))

#?(:cljs
  (if (exists? js/cljs.test$macros)
    (deftest ->file-and-line-repr-test
      (is (= ""
             (report/->file-and-line-repr "eg/test/pass.js" 5))))
    (deftest ->file-and-line-repr-test
      (is (= " (eg/test/pass.js:5)"
             (report/->file-and-line-repr "eg/test/pass.js" 5))))))

#?(:clj
  (deftest ->file-and-line-repr-test
    (is (= " (eg/test/pass.js:5)"
           (report/->file-and-line-repr "eg/test/pass.js" 5)))))

#?(:cljs
    (if (exists? js/cljs.test$macros)
      (deftest ->testing-fn-repr-test
        (is (= " (assoc-in)"
               (report/->testing-fn-repr {:function 'assoc-in :file "pass.js" :line 208}))))
      (deftest ->testing-fn-repr-test
        (is (= " (assoc-in) (pass.js:208)"
               (report/->testing-fn-repr {:function 'assoc-in :file "pass.js" :line 208}))))))

#?(:clj
  (deftest ->testing-fn-repr-test
    (is (= " (assoc-in) (pass.cljc:208)"
           (report/->testing-fn-repr {:function 'assoc-in :file "pass.cljc" :line 208})))))

(deftest normalise-pred-test
  (is (= "int?" (report/normalise-pred 'clojure.core/int?)))
  (is (= "int?" (report/normalise-pred 'cljs.core/int?)))
  (is (= "clojure.string/join" (report/normalise-pred 'clojure.string/join)))
  (is (= "clojure.string/join" (report/normalise-pred 'clojure.string/join)))
  (is (= "(fn [x] (+ x 1))" (report/normalise-pred '(fn [x] (+ x 1)))))
  (let [f '(fn [x] (+ x 1))]
    (is (= "(fn [x] (+ x 1))" (report/normalise-pred f)))))

(deftest normalise-because-error-test
  (is (= {:pred "int?", :val "\"foo\""}
         (report/normalise-because-error {:pred 'clojure.core/int?, :val "foo"}))))

(deftest spec->because-error-test
  (is (= {:pred 'clojure.core/int? :val "foo"}
         (report/spec->because-error #:clojure.spec.alpha{:problems [{:pred 'clojure.core/int?, :val "foo"}]})))
  (is (= {:pred 'clojure.core/int? :val "foo"}
         (report/spec->because-error #:cljs.spec.alpha{:problems [{:pred 'clojure.core/int?, :val "foo"}]}))))

(deftest ->because-error-msg-test
  (is (=  "\"foo\" fails: int?"
          (report/->because-error-msg {:pred 'clojure.core/int?, :val "foo"}))))

(deftest spec-because-test
  (is (= "   because: 1 fails: string?"
         (report/spec-because 1 {:pred 'clojure.core/string? :val 1} true)))
  (is (= "   because: 1 is a valid example"
         (report/spec-because 1 {:pred 'clojure.core/int? :val 1} false))))

(deftest expr-spec-test
  (is (spec/valid? ::eg-spec/expr-spec [3]))
  (is (= :one-arg (first (spec/conform ::eg-spec/expr-spec [3]))))
  (is (spec/valid? ::eg-spec/expr-spec ['(+ 1 1) 2]))
  (is (= :two-arg (first (spec/conform ::eg-spec/expr-spec ['(+ 1 1) 2]))))
  (is (spec/valid? ::eg-spec/expr-spec ['(+ 2 1) '=> 3]))
  (is (= :three-args-straight (first (spec/conform ::eg-spec/expr-spec ['(+ 2 1) '=> 3]))))
  (is (spec/valid? ::eg-spec/expr-spec [4 '= '(+ 2 2)]))
  (is (= :three-args-straight (first (spec/conform ::eg-spec/expr-spec [4 '= '(+ 2 2)]))))
  (is (spec/valid? ::eg-spec/expr-spec [4 '<= '(+ 3 1)]))
  (is (= :three-args-inverted (first (spec/conform ::eg-spec/expr-spec [4 '<= '(+ 3 1)])))))

(deftest eg-spec-example-test
  (is (spec/valid? ::eg-spec/example '([1] 1)))
  (is (= :implicit-straight-arrow (first (spec/conform ::eg-spec/example '([1] 1)))))
  (is (spec/valid? ::eg-spec/example '([1] => 1)))
  (is (= :explicit-straight-op
         (first (spec/conform ::eg-spec/example '([1] => 1)))))
  (is (spec/valid? ::eg-spec/example '([1] = 1)))
  (is (= :explicit-straight-op
         (first (spec/conform ::eg-spec/example '([1] = 1)))))
  (is (spec/valid? ::eg-spec/example '([1] <= 1)))
  (is (= :explicit-inverted-arrow
         (first (spec/conform ::eg-spec/example '([1] <= 1))))))

(deftest eg-spec-rev-example-test
  (is (spec/valid? ::eg-spec/rev-example '(1 [1])))
  (is (= :implicit-inverted-arrow (first (spec/conform ::eg-spec/rev-example '(1 [1])))))
  (is (spec/valid? ::eg-spec/rev-example '(1 <= [1])))
  (is (= :explicit-inverted-op
         (first (spec/conform ::eg-spec/rev-example '(1 <= [1])))))
  (is (spec/valid? ::eg-spec/rev-example '(1 = [1])))
  (is (= :explicit-inverted-op
         (first (spec/conform ::eg-spec/rev-example '(1 = [1])))))
  (is (spec/valid? ::eg-spec/example '([1] <= 1)))
  (is (= :explicit-straight-arrow
         (first (spec/conform ::eg-spec/rev-example '([1] => 1))))))

(deftest eg-spec-spec-example-test
  (is (spec/valid? ::eg-spec/spec-example '(! 1)))
  (is (= :spec-example-bang-one (first (spec/conform ::eg-spec/spec-example '(! 1)))))
  (is (spec/valid? ::eg-spec/spec-example '((! 1 2))))
  (is (= :spec-example-bang-variadic (first (spec/conform ::eg-spec/spec-example '((! 1 2))))))
  (is (spec/valid? ::eg-spec/spec-example '(1)))
  (is (= :spec-example (first (spec/conform ::eg-spec/spec-example '(1))))))

(deftest eg-test-test
  (is (spec/valid? ::eg-spec/eg-test '(inc [1] 2, [2] 3)))
  (is (= :function (-> (spec/conform ::eg-spec/eg-test '(inc [1] 2, [2] 3)) first)))
  (is (spec/valid? ::eg-spec/eg-test '(::fixtures/int 1 '! "eggs")))
  (is (= :spec (-> (spec/conform ::eg-spec/eg-test '(::fixtures/int 1 '! "eggs")) first))))

(deftest ge-test-test
  (is (spec/valid? ::eg-spec/ge-test '(inc 2 [1], 3 [2])))
  (is (= :function (-> (spec/conform ::eg-spec/ge-test '(inc 2 [1], 3 [2])) first)))
  (is (spec/valid? ::eg-spec/ge-test '(::fixtures/int 1 '! "eggs")))
  (is (= :spec (-> (spec/conform ::eg-spec/ge-test '(::fixtures/int 1 '! "eggs")) first))))
