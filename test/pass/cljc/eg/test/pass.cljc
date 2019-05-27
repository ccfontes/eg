(ns eg.test.pass
  (:require
    [eg.platform :refer [deftest is testing cross-throw]]
    [eg :refer [eg ge ex ->examples parse-examples test? assoc-focus-metas named-dont-care? fill-dont-cares]]))

(deftest cross-throw-test
  (is (= "BOOM" (try (cross-throw "BOOM")
                  (catch #?(:clj Exception :cljs :default) e
                    #?(:clj (-> e Throwable->map :cause))
                    #?(:cljs (.-message e)))))))

(deftest ->examples-test
  (is (= '([[2] 1]) (->examples '([2] 1))))
  (is (= '([[2] => 1]) (->examples '([2] => 1))))
  (is (= '([[2] <= 1]) (->examples '([2] <= 1))))
  (is (= '([[2] => 1], [[1] 2])
         (->examples '([2] => 1, [1] 2)))))

(deftest parse-examples-test
  (testing "should be in order: input->output"
    (is (= '([[2] 1]) (parse-examples '([[2] 1]) false)))
    (is (= '([[2] 1]) (parse-examples '([[2] => 1]) false)))
    (is (= '([[2] 1]) (parse-examples '([1 <= [2]]) false)))))

(deftest test?-test
  (is (= true  (boolean (test? (atom {:clojure.core/some false :clojure.core/any? nil})   true))))
  (is (= true  (boolean (test? (atom {:clojure.core/some true  :clojure.core/any? nil})   true))))
  (is (= true  (boolean (test? (atom {:clojure.core/some nil   :clojure.core/any? false}) nil))))
  (is (= false (boolean (test? (atom {:clojure.core/some false :clojure.core/any? true})  nil)))))

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
  #_(is (= "No choices found for don't care"
         (try (fill-dont-cares [[['_ 4] :b]])
           (catch #?(:clj Exception :cljs :default) e
             #?(:clj (-> e Throwable->map :cause))
             #?(:cljs (.-message e)))))))

; only Clojure can metadata from a function using 'meta'
#?(:clj (defmacro macro-fn-meta-fixture [f] (meta f)))
#?(:clj (deftest macro-fn-meta-simulated-test
    (is (= {:focus true} (macro-fn-meta-fixture ^:focus inc)))))

(deftest named-dont-care-test
  (is (named-dont-care? '$thing))
  (is (not (named-dont-care? 'thing)))
  (is (not (named-dont-care? "thing"))))

; this test will not run its assertions because other tests are focused
(eg true? [true] true)

(eg not [(not true)] true)

(ge * #(= 9 %) [3 3])

(eg -
  [1 2]       integer?
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
