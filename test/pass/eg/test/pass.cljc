(ns eg.test.pass
    (:require [eg.platform :as platform :refer [deftest is testing cross-throw]]
              [eg.test.fixtures :as fixtures]
              [eg :refer [eg
                          ge
                          ex
                          examples-acc
                          spec-eg-acc
                          parse-example
                          parse-expression
                          test?
                          assoc-focus-metas
                          dont-care?
                          named-dont-care?
                          fill-dont-cares
                          ->examples
                          ->test-name
                          cljs-safe-namespace
                          rm-lead-colon
                          variadic-bang?]]
              [eg.report :as report]
      #?(:clj [eg :refer [set-eg-no-refresh!]])))

(deftest examples-acc-test
  (is (= [ [] [2] ] (examples-acc [ [] [] ] 2)))
  (is (= [ [[2 1]] [] ] (examples-acc [ [] [2] ] 1)))
  (is (= [ [] [2 '=>] ] (examples-acc [ [] [2] ] '=>)))
  (is (= [ [[2 '=> 1]] [] ] (examples-acc [ [] [2 '=>] ] 1)))
  (is (= [ [] [2 '<=] ] (examples-acc [ [] [2] ] '<=)))
  (is (= [ [[2 '<= 1]] [] ] (examples-acc [ [] [2 '<=] ] 1)))
  (is (= [ [] [2 '=] ] (examples-acc [ [] [2] ] '=)))
  (is (= [ [[2 '= 1]] [] ] (examples-acc [ [] [2 '=] ] 1)))
  (is (= [ [[2 '=> 1] [1 2]] [] ] (examples-acc [ [[2 '=> 1]] [1] ] 2)))
  (is (= [ [] [nil] ] (examples-acc [ [] [] ] nil)))
  (is (= [ [[nil nil]] [] ] (examples-acc [ [] [nil] ] nil)))
  (is (= [ [] ['=>] ] (examples-acc [ [] [] ] '=>))))

(deftest variadic-bang?-test
  (is (not (variadic-bang? 1)))
  (is (not (variadic-bang? '())))
  (is (not (variadic-bang? '("a"))))
  (is (not (variadic-bang? '(!))))
  (is (variadic-bang? '(! 4)))
  (is (variadic-bang? '(! 4 "b"))))

(deftest spec-eg-acc-test
  (is (= [ [[2]] ['!] ] (spec-eg-acc [ [[2]] [] ] '!)))
  (is (= [ [[2] ['! 3] ['! 4]] [] ] (spec-eg-acc [ [[2]] [] ] '(! 3 4))))
  (is (= [ [[2] [3]] [] ] (spec-eg-acc [ [[2]] [] ] 3)))
  (is (= [ [[2] ['! 3]] [] ] (spec-eg-acc [ [[2]] ['!] ] 3))))

(deftest parse-example-test
  (testing "should be in order: input->output"
    (is (= [[2] '=> 1] (parse-example [[2] 1] false)))
    (is (= [[2] '=> 1] (parse-example [1 [2]] true)))
    (is (= [[2] '=> 1] (parse-example [[2] '=> 1] false)))
    (is (= [[2] '=> 1] (parse-example [[2] '=> 1] true)))
    (is (= [[2] '=> 1] (parse-example [1 '<= [2]] false)))
    (is (= [[2] '=> 1] (parse-example [1 '<= [2]] true)))
    (is (= [[inc] '= inc] (parse-example [[inc] '= inc] false)))
    (is (= [[inc] '= inc] (parse-example [inc '= [inc]] true)))
    (is (= [[2] '=> 1] (parse-example [1 '<= 2] false)))
    (is (= [[2] '=> 1] (parse-example [2 '=> 1] true)))
    (is (= [[2] '=> 1] (parse-example [1 2] true)))
    (is (= "eg examples need to come in pairs, but found only: '[2]'"
         (try (parse-example [[2]] false)
           (catch #?(:clj Exception :cljs :default) e
             #?(:clj (-> e Throwable->map :cause))
             #?(:cljs (.-message e))))))))

(deftest parse-expression-test
  (is (= [4 '=> 2] (parse-expression [4 '=> 2])))
  (is (= [3 '=> 2] (parse-expression [2 '<= 3])))
  (is (= [2 '= 5] (parse-expression [2 '= (+ 1 4)])))
  (is (= "Was expecting an arrow, but found '3' instead.."
         (try (parse-expression [2 3])
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
  (is (= [[[1 2] :a] [[1 4] :b] [[5 6] :c]]
         (fill-dont-cares [[[1 2] :a] [['_ 4] :b] [[5 6] :c]])))
  (is (= [[[1 2] :a] [[1 4] :b] [[5] :c]]
         (fill-dont-cares [[[1 2] :a] [['_ 4] :b] [[5] :c]])))
  (is (= [[[5 2] :b] [[5 2] [{:a 5} 5]]]
         (fill-dont-cares [[[5 2] :b] [['$1 2] [{:a '$1} '$1]]])))
  (is (= [[[5 2] '= :b] [[5 2] '=> [{:a 5} 5]]]
         (fill-dont-cares [[[5 2] '= :b] [['$1 2] '=> [{:a '$1} '$1]]])))
  (is (= [[[1 2] nil] [[1 4] :b]]
         (fill-dont-cares [[[1 2] nil] [['_ 4] :b]])))
  (is (= "No choices found for don't care: _"
         (try (doall (fill-dont-cares [[['_ 4] :b]]))
           (catch #?(:clj Exception :cljs :default) e
             #?(:clj (-> e Throwable->map :cause))
             #?(:cljs (.-message e)))))))

(deftest ->examples-test
  (is (= [[[0] '=> false?] [[1] '=> 2]] (->examples 'inc true [false? [0] 2 [1]])))
  (is (= [[3] ['! 5]] (->examples ::fixtures/int false [3 '! 5])))
  (is (= "Not a valid test name type: inc"
         (try (->examples "inc" false [[0]])
           (catch #?(:clj Exception :cljs :default) e
             #?(:clj (-> e Throwable->map :cause))
             #?(:cljs (.-message e)))))))

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
  #?(:cljs (do (is (= "cljs" (platform/if-cljs "cljs" "clj")))
               (is (= "cljs" (platform/if-cljs "cljs"))))
     :clj (do (is (= "clj" (platform/if-cljs "cljs" "clj")))
              (is (= nil (platform/if-cljs "clj"))))))

(deftest equal?-test
  (is (true? (platform/equal? 3 3)))
  (is (= (= 3 3) (platform/equal? 3 3))))

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
      (is (= ["eg/test/pass.js"]
             (report/->file-and-line-repr "eg/test/pass.js" 5))))
    (deftest ->file-and-line-repr-test
      (is (= ["eg/test/pass.js:5"]
             (report/->file-and-line-repr "eg/test/pass.js" 5))))))

#?(:clj
  (deftest ->file-and-line-repr-test
    (is (= ["eg/test/pass.js:5"]
           (report/->file-and-line-repr "eg/test/pass.js" 5)))))

#?(:cljs
  (if (exists? js/cljs.test$macros)
    (deftest ->testing-fn-repr-test
      (is (= [['assoc-in] ["pass.js"]]
             (report/->testing-fn-repr {:function 'assoc-in :file "pass.js" :line 208 :expression? false}))))
    (deftest ->testing-fn-repr-test
      (is (= ["pass.js:208"]
             (report/->testing-fn-repr {:function 'assoc-in :file "pass.js" :line 208 :expression? true}))))))

#?(:clj
  (deftest ->testing-fn-repr-test
    (is (= [['assoc-in] ["pass.cljc:208"]]
           (report/->testing-fn-repr {:function 'assoc-in :file "pass.cljc" :line 208 :expression? false})))
    (is (= ["pass.cljc:208"]
           (report/->testing-fn-repr {:function 'assoc-in :file "pass.cljc" :line 208 :expression? true})))))

(deftest normalise-pred-test
  (is (= "int?" (report/normalise-pred 'clojure.core/int?)))
  (is (= "int?" (report/normalise-pred 'cljs.core/int?)))
  (is (= "clojure.string/join" (report/normalise-pred 'clojure.string/join))))

(deftest normalise-because-error-test
  (is (= {:pred "int?", :val "\"foo\""}
         (report/normalise-because-error {:pred 'clojure.core/int?, :val "foo"}))))

(deftest spec->because-error-test
  (is (= {:pred 'clojure.core/int? :val "foo"}
         (report/spec->because-error #:clojure.spec.alpha{:problems [{:pred 'clojure.core/int?, :val "foo"}]})))
  (is (= {:pred 'clojure.core/int? :val "foo"}
           (report/spec->because-error #:cljs.spec.alpha{:problems [{:pred 'clojure.core/int?, :val "foo"}]}))))

(deftest ->because-error-msg-test
  (is (=  "\"foo\" fails: int?" (report/->because-error-msg {:pred 'clojure.core/int?, :val "foo"}))))

(deftest spec-because-test
  (is (= "   because: 1 fails: string?"
         (report/spec-because 1 {:pred 'clojure.core/string? :val 1} true)))
  (is (= "   because: 1 is a valid example"
         (report/spec-because 1 {:pred 'clojure.core/int? :val 1} false))))

(eg true? true true)

(eg fixtures/noargs [] "foo")

(eg not [(not true)] true)

(eg set [[1 2]] #{1 2})

(ge * #(= 9 %) [3 3])

(eg -
  [1 2]       integer?
  [1 2]       ::fixtures/int
  [1 2]    => -1
  integer? <= [1 2])

(ge +
  3           [1 2]
  [1 2]    => #(integer? %)
  integer? <= [1 2])

(let [test-eg-ret (ex (inc 0) => 1)
      f-len (count "eg-test-")]
  (ex var? <= test-eg-ret)
  (ex (-> test-eg-ret meta :test) => boolean)
  (ex (-> test-eg-ret meta :test) => fn?)
  (ex (-> test-eg-ret meta :name name (subs f-len)) => not-empty))

(ex (true? false) => false)

; TODO visit (ex nil) (ex false)

(eg clojure.core/false? [false] true)

(eg vector
  [5 6 _ 8]   vector?
  [4 _ 5]     vector?
  [3 $mono]   vector?
  [$thing $2] [$thing $2])

(eg assoc-in
  [{} [:a :b] {:eggs "boiled"}] => {:a {:b {:eggs "boiled"}}}
  [_ $spam _] => map?
  [_ _ $eggs] => {:a {:b $eggs}})

#?(:clj
  (let [set-eg-ret (set-eg-no-refresh! '[eg ge])]
    (ex '#{eg ge} <= (set (map (comp :name meta) set-eg-ret)))))

(eg ::fixtures/string "foo" (! 2 3))

(ge ::fixtures/int
  (identity 4)
  !"eggs"
  (! 4.5 2.3)
  3)

(eg ::fixtures/map {:foo "bar"})

(eg fixtures/foo 2 = inc)

(ge fixtures/bar inc = 2)

(eg identity
  nil   nil
  1 => ::fixtures/int
  "eggs" ::fixtures/string
  ::fixtures/string <= "foo"
  ::fixtures/int = ::fixtures/int
  [nil] nil)

(ex (identity nil) => nil)

(ex (fixtures/foo 2) = inc)

(ex inc = (fixtures/foo 2))

#?(:cljs
  (eg fixtures/js-eggs
    #js {:a [1]} => #js {:a [1]}
    #js {:a [1]} => (clj->js {:a [1]})
    (clj->js {:a [1]}) => #js {:a [1]}
    (clj->js {:a [1]}) => (clj->js {:a [1]})))

#?(:cljs
  (ex (fixtures/js-eggs #js {:a [2]}) => #js {:a [2]}))

(ex 4 => int?)
