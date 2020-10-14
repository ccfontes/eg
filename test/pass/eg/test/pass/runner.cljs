(ns eg.test.pass.runner
  (:require [eg.test.pass.unit]
            [eg.test.pass.integration]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.pass.unit 'eg.test.pass.integration)
