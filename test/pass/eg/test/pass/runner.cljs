(ns eg.test.pass.runner
  (:require [eg.test.report]
            [eg.test.pass]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.report
           'eg.test.pass)
