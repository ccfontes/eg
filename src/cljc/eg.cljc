(ns eg
  #?(:cljs (:require-macros [eg :refer [eg ge]]))
  (:require
    #?(:clj [clojure.test :as test]
       :cljs [cljs.test :include-macros true])))

(defonce registry-ref (atom {}))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs.
  source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
  source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro is
  "source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-cljs
     (cljs.test/is ~@args)
     (clojure.test/is ~@args)))

(defmacro deftest
  "source: http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing"
  [& args]
  `(if-cljs
     (cljs.test/deftest ~@args)
     (clojure.test/deftest ~@args)))

(defmacro example [fn-sym example-pairs]
  (let [test-name (-> fn-sym name (str "-test") symbol)
        fully-qualified-fn-name `fn-sym]
    (swap! registry-ref assoc fully-qualified-fn-name example-pairs)
    `(deftest ~test-name
       ~@(map (fn [[param-vec ret]]
                `(if (fn? ~ret)
                  (is (~ret (~fn-sym ~@param-vec)))
                  (is (= ~ret (~fn-sym ~@param-vec)))))
              example-pairs))))

(defmacro eg [fn-sym & examples]
  (let [example-pairs (->> examples (partition 2))]
   `(example ~fn-sym ~example-pairs)))

(defmacro ge [fn-sym & examples]
  (let [example-pairs (->> examples (partition 2) (map reverse))]
   `(example ~fn-sym ~example-pairs)))
