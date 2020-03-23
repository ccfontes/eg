(ns eg.test.fail.runner
  (:require [eg.test.fail]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.fail)
