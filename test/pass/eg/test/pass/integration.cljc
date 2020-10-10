(ns eg.test.pass.integration
   ^{:author "Carlos da Cunha Fontes"
     :license {:name "The MIT License"
               :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}
     :doc "Purpose is to test 'eg', 'ge', and 'ex' forms, and to do so in
          isolation from the rest of the test code as much as possible, so as
          to catch errors like ones arising from missing namespace requires
          when using this library."}
  (:require [eg :refer [eg ge ex]]
            [eg.test.fixtures :as fixtures]
    #?(:clj [eg :refer [set-eg-no-refresh!]])))

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

(let [test-eg-ret (ex (inc 0) => 1)
      f-len (count "eg-test-")]
  (ex var? <= test-eg-ret)
  (ex (-> test-eg-ret meta :test) => boolean)
  (ex (-> test-eg-ret meta :test) => fn?)
  (ex (-> test-eg-ret meta :name name (subs f-len)) => not-empty))

(ex (identity nil) => nil)

(ex (identity nil) = nil)

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

(ex (true? false) => false)

(ex false => #(false? %))

(ex "foo")

(let [f (constantly true)]
  (ex "foo" => f))

; prove that expressions are evaluated inside 'deftest'
(ex fixtures/exception-report => #(re-find #"ERROR in" %))
; prove that expressions are evaluated inside 'is', because "expected:" is not nil
(ex fixtures/exception-report => #(re-find #"expected: \(eg.platform/pred-ex \(string" %))
