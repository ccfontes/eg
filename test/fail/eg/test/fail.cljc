(ns eg.test.fail
  (:require [clojure.spec.alpha :as spec]
            [eg :refer [eg ge ex]]
    #?(:clj [eg :refer [set-eg-no-refresh!]])))

(defn foo [x] inc)

(spec/def ::string string?)

(spec/def ::int int?)

(eg not [(not true)] false)

(ge + #(= 9 %) [3 7])

(eg *
  [2]   1
  [3 2] string?)

(ge -
  1 [1]
  2 [2 4])

(eg ::int
  "spam"
  ! 4
  "eggs")

(ge :eg.test.fail/string
  4
  ! "foo"
  3)

(eg foo 2 = dec)

(ex (true? true) => false)

(ex string? <= (inc 0))

(ex (foo 2) = inc)

(ex inc = (foo 2))

(ex nil)

(ex false)

(eg identity
  1 => ::string
  "eggs" ::int
  ::int <= "foo"
  1 = ::int)
