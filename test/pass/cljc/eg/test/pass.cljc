(ns eg.test.pass
    (:require [clojure.spec.alpha :as spec]
              [eg.platform :refer [deftest is testing cross-throw]]
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
      #?(:clj [eg :refer [set-eg-no-refresh!]])))

(defn foo [x] inc)

(defn bar [x] inc)

(defn noargs [] "foo")

(defn js-eggs [x] x)

(spec/def ::string string?)

(spec/def ::int int?)

(spec/def ::map map?)

(deftest cross-throw-test
  (is (= "BOOM" (try (cross-throw "BOOM")
                  (catch #?(:clj Exception :cljs :default) e
                    #?(:clj (-> e Throwable->map :cause))
                    #?(:cljs (.-message e)))))))

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
  (is (symbol? (->test-name ::int)))
  (is (= 'inc-test (->test-name 'inc)))
  (is (= 'clojure-core-inc-test (->test-name 'clojure.core/inc)))
  (is (= 'eg-test-pass-:int-test (->test-name ::int))))

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
  (is (= [[3] ['! 5]] (->examples ::int false [3 '! 5])))
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

(eg true? true true)

(eg noargs [] "foo")

(eg not [(not true)] true)

(eg set [[1 2]] #{1 2})

(ge * #(= 9 %) [3 3])

(eg -
  [1 2]       integer?
  [1 2]       ::int
  [1 2]    => -1
  integer? <= [1 2])

(ge +
  3           [1 2]
  [1 2]    => #(integer? %)
  integer? <= [1 2])

(let [test-eg-ret (ex (inc 0) 1)
      f-len (count "eg-test-")]
  (ex var? <= test-eg-ret
      (-> test-eg-ret meta :test) => boolean
      (-> test-eg-ret meta :test) => fn?
      (-> test-eg-ret meta :name name (subs f-len)) => not-empty))

(ex (true? false) => false)

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

(eg ::string "foo")

(ge :eg.test.pass/int
  (identity 4)
  !"eggs"
  (! 4.5 2.3)
  3)

(eg ::map {:foo "bar"})

(eg foo 2 = inc)

(ge bar inc = 2)

(eg identity
  nil   nil
  1 => ::int
  "eggs" ::string
  ::string <= "foo"
  ::int = ::int
  [nil] nil)

(ex (identity nil) => nil)

(ex (foo 2) = inc)

(ex inc = (foo 2))

#?(:cljs
  (eg js-eggs
    #js {:a [1]} => #js {:a [1]}
    #js {:a [1]} => (clj->js {:a [1]})
    (clj->js {:a [1]}) => #js {:a [1]}
    (clj->js {:a [1]}) => (clj->js {:a [1]})))

#?(:cljs
  (ex (js-eggs #js {:a [2]}) => #js {:a [2]}))
