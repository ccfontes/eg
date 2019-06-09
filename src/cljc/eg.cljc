(ns eg ^{:author "Carlos da Cunha Fontes"
         :license {:name "The Universal Permissive License (UPL), Version 1.0"
                   :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [eg.platform :refer [deftest is cross-throw]]
            [clojure.walk :refer [postwalk]]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
   #?(:cljs [cljs.test :include-macros true])
  #?@(:clj [[clojure.test :as clj.test]
            [clojure.tools.namespace.repl]]))
  #?(:cljs (:require-macros [eg :refer [eg ge ex]])))

(defonce focus-metas (atom {}))

(defn map-dregs
  "Like map but when there is a different count between colls, applies input fn
   to the coll values until the biggest coll is empty."
  [f & colls]
  ((fn map* [f colls]
     (lazy-seq
       (if-let [non-empty-colls (seq (filter seq colls))]
         (let [first-items (map first non-empty-colls)
               rest-colls (map rest non-empty-colls)]
           (cons (apply f first-items)
                 (map* f rest-colls))))))
  f colls))

(defn examples-acc [[parts part] token]
  (let [new-part (conj part token)]
    (if (or (empty? part) (#{'=> '<=} token))
      [parts new-part]
      [(conj parts new-part) []])))

(defn parse-example [example ge?]
  (let [[params exp]
          (if (#{2 3} (count example))
            (let [pair [(first example) (last example)]]
              (if (or (and ge? (not= (second example) '=>))
                      (= (second example) '<=))
                (reverse pair)
                pair))
            (let [egge (str (if ge? "ge" "eg"))]
              (cross-throw (str egge " examples need to come in pairs, but found only: '" (first example) "'"))))
        normalized-params (if (vector? params) params [params])]
    [normalized-params exp]))

(defn parse-expression [expr]
  (let [parsed [(first expr) (last expr)]
        arrow (nth expr 1)]
    (if (= arrow '=>)
      parsed
      (if (= arrow '<=)
        (reverse parsed)
        (cross-throw (str "Was expecting an arrow, but found '" arrow "' instead.."))))))

(defn test? [focus-metas focus?]
  (let [focuses (vals @focus-metas)
        focuses? (some true? focuses)]
    (boolean
      (or focus? (not focuses?)))))

(defn rm-lead-colon
  "Motivation: 'name' does not work for processed strings"
  [s] (if (= ":" (-> s first str))
        (subs s 1)
        s))

(defn cljs-safe-namespace
  "Motivation: Using '.' in the name causes compilation error in cljs."
  [thing]
  (let []
    (-> thing str (str/replace "." "-") symbol namespace rm-lead-colon)))

(def ->test-name
  (comp symbol
        #(str % "-test")
        #(str/join "-" %)
        #(keep identity %)
        (juxt cljs-safe-namespace
              #(str (if (keyword? %) ":") (name %)))))

(defmacro ->example-test [fn-sym examples focus-metas- focus?]
  (let [test-name (->test-name fn-sym)]
    `(let [test# (deftest ~test-name
                   (when (test? ~focus-metas- ~focus?)
                     ~@(map (fn [example]
                              (let [[param-vec ret] (if (coll? example) example)]
                                (if (qualified-keyword? fn-sym)
                                  `(is (spec/valid? ~fn-sym ~example))
                                  `(if (fn? ~ret)
                                    (is (~ret (~fn-sym ~@param-vec)))
                                    (is (= ~ret (~fn-sym ~@param-vec)))))))
                            examples)))]
      ; passing down ^:focus meta to clojure.test: see alter-test-var-update-fn
      ; FIXME not associng in cljs
      (alter-meta! (var ~test-name) #(assoc % :focus ~focus?))
      test#)))

(defmacro ->expression-test [examples]
  (let [rand-id (int (* (rand) 100000))
        test-name (symbol (str "eg-test-" rand-id))]
    `(deftest ~test-name
      ~@(map (fn [[res expected]]
               `(if (fn? ~expected)
                 (is (~expected ~res))
                 (is (= ~expected ~res))))
             examples))))

(defn assoc-focus-metas [focus-metas- fn-meta fn-sym]
  (let [fn-ns-name (-> fn-meta :ns str)
        qualified-fn-kw (keyword (str fn-ns-name "/" fn-sym))
        focus? (:focus fn-meta)]
    (assoc focus-metas- qualified-fn-kw focus?)))

; FIXME function not executing in cljs
(defn alter-test-var-update-fn [test-v]
  (fn [v]
    (let [focus? (-> v meta :focus)]
      (if (test? focus-metas focus?)
        (test-v v)))))

(defn named-dont-care? [thing]
  (and (symbol? thing)
       (= \$ (-> thing name first))))

(def dont-care? (some-fn #{'_} named-dont-care?))

(defn fill-dont-cares [examples]
  (let [input-examples (map first examples)
        choices-per-param (apply map-dregs #(->> %& (remove dont-care?) vec) input-examples)
        fo (fn [[params exp]]
             ; OPTIMIZE to choose at random
             (let [fi (fn [[param-acc exp] [param choices]]
                        (if (dont-care? param)
                          (if-let [choice (first choices)]
                            (let [pw-f #(if (= param %) choice %)]
                              [(concat param-acc [choice])
                               (if (named-dont-care? param) (postwalk pw-f exp) exp)])
                            (cross-throw "No choices found for don't care")) ; TODO add don't care name
                          [(concat param-acc [param]) exp]))]
               (reduce fi [[] exp] (map #(vec %&) params choices-per-param))))]
    (map fo examples)))

(defn ->examples [test-thing ge? body]
  (cond
    (symbol? test-thing)
      (->> body
        (reduce examples-acc [[] []])
        (first)
        (map #(parse-example % ge?))
        (fill-dont-cares))
    (keyword? test-thing) body
    :else (cross-throw (str "Not a valid test name type: " test-thing))))

(defmacro eg-helper [[fn-sym & body] ge?]
  (let [examples (->examples fn-sym ge? body)
        fn-meta (meta fn-sym)
        focus? (:focus fn-meta)]
    `(do (swap! focus-metas assoc-focus-metas ~fn-meta ~fn-sym)
         (->example-test ~fn-sym ~examples focus-metas ~focus?))))

(defmacro eg [& args] `(eg-helper ~args false))

(defmacro ge [& args] `(eg-helper ~args true))

(defmacro ex [& body]
  (let [examples (->> body (partition 3) (map parse-expression))]
    `(->expression-test ~examples)))

#?(:clj
  (alter-var-root (var clj.test/test-var) alter-test-var-update-fn))

#?(:cljs ; FIXME this is not redefining 'test-var'
  (set! cljs.test/test-var (alter-test-var-update-fn cljs.test/test-var)))

#?(:clj
  (defn set-eg-no-refresh! []
    (let [eg-var (intern 'clojure.core (with-meta 'eg {:macro true}) @#'eg)
          ge-var (intern 'clojure.core (with-meta 'ge {:macro true}) @#'ge)
          ex-var (intern 'clojure.core (with-meta 'ex {:macro true}) @#'ex)]    
      #{eg-var ge-var ex-var})))

#?(:clj ; FIXME cannot be tested – calling clojure.tools.namespace.repl/refresh causes lein test to run 0 tests
  (defn set-eg! []
    (set-eg-no-refresh!)
    (clojure.tools.namespace.repl/refresh)))
