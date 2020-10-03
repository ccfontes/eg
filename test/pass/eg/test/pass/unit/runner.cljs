(ns eg.test.pass.unit.runner
  (:require [eg.test.pass.unit]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.pass.unit)
