(ns eg ^{:author "Carlos da Cunha Fontes"
         :license {:name "The Universal Permissive License (UPL), Version 1.0"
                   :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [eg.platform :refer [deftest is cross-throw]])
  #?(:cljs (:require-macros [eg :refer [eg ge]])))

(defonce focus-metas (atom {}))

(defn ->examples [egge-body]
  (first
    (reduce
      (fn [[parts part] token]
        (let [new-part (conj part token)]
          (if (#{'=> '<=} token)
            (if (= (count part) 2)
              [(conj parts new-part) []]
              [parts new-part])
            (if (empty? part)
              [parts new-part]
              [(conj parts new-part) []]))))
      [[] []]
      egge-body)))

(defn parse-examples [examples & [ge?]]
  (map #(if (= (count %) 3)
          (let [pair [(first %) (last %)]]
            (if (= (nth % 1) '<=) (reverse pair) pair))
          (if (= (count %) 1)
            (let [egge (str (if ge? "ge" "eg"))]
              (cross-throw (str egge " examples need to come in pairs.")))
            (if ge? (reverse %) %)))
       examples))

(defn test? [focus-metas focus?]
  (let [focuses (vals @focus-metas)
        focuses? (some true? focuses)]
    (boolean
      (or focus?
          (and (not focus?) (not focuses?))))))

(defmacro ->example-test [fn-sym examples focus-metas- focus?]
  (let [test-name (-> fn-sym name (str "-test") symbol)]
    `(deftest ~test-name
      (when (test? ~focus-metas- ~focus?)
        ~@(map (fn [[param-vec ret]]
                 `(if (fn? ~ret)
                   (is (~ret (~fn-sym ~@param-vec)))
                   (is (= ~ret (~fn-sym ~@param-vec)))))
               examples)))))

(defn assoc-focus-metas [focus-metas- fn-meta fn-sym]
  (let [fn-ns-name (-> fn-meta :ns str)
        qualified-fn-kw (keyword (str fn-ns-name "/" fn-sym))
        focus? (:focus fn-meta)]
    (assoc focus-metas- qualified-fn-kw focus?)))

(defmacro eg [fn-sym & body]
  (let [examples (-> body ->examples parse-examples)
        fn-meta (meta fn-sym)
        focus? (:focus fn-meta)]
    `(do (swap! focus-metas assoc-focus-metas ~fn-meta ~fn-sym)
         (->example-test ~fn-sym ~examples focus-metas ~focus?))))

(defmacro ge [fn-sym & body]
  (let [examples (-> body ->examples (parse-examples true))
        fn-meta (meta fn-sym)
        focus? (:focus fn-meta)]
    `(do (swap! focus-metas assoc-focus-metas ~fn-meta ~fn-sym)
         (->example-test ~fn-sym ~examples focus-metas ~focus?))))
