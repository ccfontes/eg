(ns eg.test.fail
  (:require [eg.core :refer [eg ge]]))

(eg not [(not true)] false)

(ge + #(= 9 %) [3 7])

(eg *
  [2]   1
  [3 2] string?)

(ge -
  1 [1]
  2 [2 4])
