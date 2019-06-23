(ns eg.platform
  #?(:cljs (:require-macros [eg.platform :refer [deftest is testing]]))
  (:require [clojure.test :as clj.test]
            #?(:cljs [cljs.test :include-macros true])))

(defn cross-throw [msg]
  (throw #?(:cljs (js/Error. msg)
            :clj (Exception. msg))))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs.
  Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
  Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [then else]
  (if (cljs-env? &env) then else))

(defn ->clj [datum]
  #?(:clj datum)
  #?(:cljs (js->clj datum)))

(defmacro is
  "Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-cljs
     (cljs.test/is ~@args)
     (clj.test/is ~@args)))

(defmacro deftest
  "Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-cljs
     (cljs.test/deftest ~@args)
     (clj.test/deftest ~@args)))

(defmacro testing [& args]
  `(if-cljs
     (cljs.test/testing ~@args)
     (clj.test/testing ~@args)))
