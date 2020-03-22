(ns eg.test.report
  (:require [clojure.test :refer [deftest is]]
   #?(:cljs [eg.report :as report])))

#?(:cljs
  (deftest rm-cljsjs-st-fname-prefix-fluff-test
    (is (= "eg/test/pass.js:2590:27"
           (report/rm-cljsjs-st-fname-prefix-fluff "cljs$lang$test@file:///eg/test/pass.js:2590:27")))))
