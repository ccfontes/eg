(ns eg.test.pass
  (:require [eg :refer [eg ge]]))

(eg not [(not true)] true)

(ge + #(= 10 %) [3 7])

(eg *
  [2]   2
  [3 2] integer?)

(ge -
  -1 [1]
  -2 [2 4])
