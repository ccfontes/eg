(ns eg.test.fail
  (:require [eg :refer [eg ge ex]]))

(eg not [(not true)] false)

(ge + #(= 9 %) [3 7])

(eg *
  [2]   1
  [3 2] string?)

(ge -
  1 [1]
  2 [2 4])

(ex (true? true) => false)

(ex string? <= (ex (inc 0) 1))
