(ns eg.test.platform
  (:require [clojure.test :refer [deftest is]]
   #?(:cljs [eg.platform :as plat])))

#?(:cljs
  (deftest rm-cljsjs-st-fname-prefix-fluff-test
    (is (= "eg/test/pass.js:2590:27"
           (plat/rm-cljsjs-st-fname-prefix-fluff "cljs$lang$test@file:///eg/test/pass.js:2590:27")))))
