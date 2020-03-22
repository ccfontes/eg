(ns eg.platform
  #?(:cljs (:require-macros [eg.platform :refer [deftest is testing]]))
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as spec]
            [clojure.test :as clj.test]
            [cljs.test :include-macros true]))

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

(defn valid-spec?
  "Solves a clojure.spec.alpha/valid? resolve issue in cljs JVM.
  Check client code for use cases."
  [& args] (apply spec/valid? args))

(defn invalid-spec?
  "Complement of valid-spec? to able a distinct dispatch fn arg in clojure.test/assert-expr."
  [& args] (not (apply spec/valid? args)))

(defn equal?
  "Create alias for '=, so that we don't override or be overriden by libraries dispatching on '= for assert-expr, 
  and to apply our custom assert-expr only to function tests, i.e., not expression tests."
  [& args] (apply = args))

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
