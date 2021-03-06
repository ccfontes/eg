(ns eg.platform ^{:author "Carlos da Cunha Fontes"
                  :license {:name "The MIT License"
                            :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  #?(:cljs (:require-macros [eg.platform :refer [deftest is testing]]))
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as spec]
   #?(:clj  [clojure.test :as clj.test]
      :cljs [cljs.test :include-macros true])))

(defn cross-throw [msg]
  (throw #?(:cljs (js/Error. msg)
            :clj (Exception. msg))))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs.
  Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [env]
  (boolean (:ns env)))

(defmacro if-target-is-cljs
  "Return then if we are generating cljs code and else for Clojure code.
  Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [then & [else]] (if (cljs-env? &env) then else))

(defn ->clj
  "Recursively transform JS arrays into cljs vectors, and JS objects into cljs
  maps, otherwise resulting data structure will be untouched.
  We need this in order to compare for equality 'actual' JS arrays or objects,
  with 'expected' ones in assertions."
  [datum]
  #?(:clj datum)
  #?(:cljs (js->clj datum)))

(def cross-invalid-spec-kw
  #?(:clj :clojure.spec.alpha/invalid)
  #?(:cljs :cljs.spec.alpha/invalid))

(defn valid-spec?
  "Solves a clojure.spec.alpha/valid? resolve issue in cljs JVM.
  Check client code for use cases."
  [& args] (apply spec/valid? args))

(defn invalid-spec?
  "Complement of valid-spec? to able a distinct dispatch fn arg in clojure.test/assert-expr."
  [& args] (not (apply spec/valid? args)))

(defn valid-expected-spec?
  "Play fairly with other libraries dispatching with clojure.spec.alpha/valid? for clojure.test/assert-expr."
  [& args] (apply spec/valid? args))

(defn valid-expected-spec-ex?
  "Same as 'valid-expected-spec?'"
  [& args] (apply spec/valid? args))

(defn equal-eg?
  "Create a two args version of '=, so that we don't override or be overriden
  by libraries dispatching on '= for clojure.test/assert-expr, and to apply
  our custom clojure.test/assert-expr only to function tests, i.e., not
  expression tests."
  [x y] (= (->clj x) (->clj y)))

(defn equal-ex?
  "The same as 'equal-eg?', but to be used for expression tests."
  [x y] (equal-eg? x y))

(defn pred-eg
  "Meant to be used as a clojure.test/assert-expr dispatch value which is a predicate 'eg' checker."
  [arg] arg)

(defn pred-ex
  "Same as 'eg', but called from an expression test."
  [arg] arg)

(defn explain-data
  "By wrapping spec/explain-data inside this function, prevents the macro using this code
  from expanding the wrong version of explain-data - clojure.spec.alpha/explain-data."
  [& args] (apply spec/explain-data args))

(defmacro is
  "Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-target-is-cljs
     (cljs.test/is ~@args)
     (clj.test/is ~@args)))

(defmacro deftest
  "Source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-target-is-cljs
     (cljs.test/deftest ~@args)
     (clj.test/deftest ~@args)))

(defmacro testing [& args]
  `(if-target-is-cljs
     (cljs.test/testing ~@args)
     (clj.test/testing ~@args)))
