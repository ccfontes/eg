(ns eg.test.pass.runner
  (:require [eg.test.platform]
            [eg.test.pass]
            [cljs.test :refer [run-tests]]))

(enable-console-print!)

(run-tests 'eg.test.platform
           'eg.test.pass)
