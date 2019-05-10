(ns eg ^{:author "Carlos da Cunha Fontes"
         :license {:name "The Universal Permissive License (UPL), Version 1.0"
                   :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [eg.platform :refer [deftest is]])
  #?(:cljs (:require-macros [eg :refer [eg ge]])))

(defonce registry-ref (atom {}))

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
              (throw (#?(:cljs js/Error. :clj Exception.
                      (str egge " examples need to come in pairs.")))))
            (if ge? (reverse %) %)))
       examples))

(defmacro ->example-test [fn-sym examples]
  (let [test-name (-> fn-sym name (str "-test") symbol)
        qualified-fn-name `fn-sym]
    (swap! registry-ref assoc qualified-fn-name examples)
    `(deftest ~test-name
       ~@(map (fn [[param-vec ret]]
                `(if (fn? ~ret)
                  (is (~ret (~fn-sym ~@param-vec)))
                  (is (= ~ret (~fn-sym ~@param-vec)))))
              examples))))

(defmacro eg [fn-sym & body]
  (let [examples (-> body ->examples parse-examples)]
   `(->example-test ~fn-sym ~examples)))

(defmacro ge [fn-sym & body]
  (let [examples (-> body ->examples (parse-examples true))]
   `(->example-test ~fn-sym ~examples)))
