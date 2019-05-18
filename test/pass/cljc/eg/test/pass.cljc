(ns eg.test.pass
  (:require
    [eg.platform :refer [deftest is testing]]
    [eg :refer [eg ge ->examples parse-examples test? assoc-focus-metas]]))

(deftest ->examples-test
  (is (= '([[2] 1]) (->examples '([2] 1))))
  (is (= '([[2] => 1]) (->examples '([2] => 1))))
  (is (= '([[2] <= 1]) (->examples '([2] <= 1))))
  (is (= '([[2] => 1], [[1] 2])
         (->examples '([2] => 1, [1] 2)))))

(deftest parse-examples-test
  (testing "should be in order: input->output"
    (is (= '([[2] 1]) (parse-examples '([[2] 1]))))
    (is (= '([[2] 1]) (parse-examples '([[2] => 1]))))
    (is (= '([[2] 1]) (parse-examples '([1 <= [2]]))))))

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

; only Clojure can metadata from a function using 'meta'
#?(:clj (defmacro macro-fn-meta-fixture [f] (meta f)))
#?(:clj (deftest macro-fn-meta-simulated-test
    (is (= {:focus true} (macro-fn-meta-fixture ^:focus inc)))))

; this test will not run its assertions because other tests are focused
(eg true? [true] true)

(eg not [(not true)] true)

(ge * #(= 9 %) [3 3])

(eg -
  [1 2]       integer?
  [1 2]    => -1
  integer? <= [1 2])

(ge +
  3          [1 2]
  [1 2]    => #(integer? %)
  integer? <= [1 2])

; uncomment below after supporting expression testing
; 'ex' name is likely to change
#_(let [test-eg-ret (eg inc [0] 1)]
  (ex test-eg-ret         fn?
      (:test test-eg-ret) boolean
      (:test test-eg-ret) fn?))

(eg clojure.core/false? [false] true)
