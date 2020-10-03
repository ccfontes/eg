(ns eg.test.pass.integration.runner
  (:require [eg.test.pass.integration]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.pass.integration)
