(ns eg ^{:author "Carlos da Cunha Fontes"
         :license {:name "The MIT License"
                   :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  #?(:cljs (:require-macros [eg :refer [eg ge ex]]))
  (:require [eg.platform :refer [deftest
                                 is
                                 cross-throw
                                 cross-invalid-spec-kw
                                 valid-spec?
                                 invalid-spec?
                                 equal-eg?
                                 equal-ex?
                                 valid-expected-spec?
                                 valid-expected-spec-ex?
                                 pred-eg
                                 pred-ex]]
            [eg.spec :as eg-spec]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [clojure.test :as clj.test]
            [clojure.spec.alpha :as spec]
   #?(:cljs [cljs.test :include-macros true])
    #?(:clj [clojure.tools.namespace.repl])
   #?@(:bb  [eg.report.clj]
       :clj [[eg.report.clj]     ; here for side-effects to extend clojure.test/assert-expr
             [eg.report.cljs]]   ; here for side-effects to extend cljs.test/assert-expr
       :cljs [eg.report.cljs]))) ; here for side-effects to extend cljs.test/report, and js/cljs.test$macros.assert_expr 

(defonce focus-metas (atom {}))

(def operators #{'=> '<= '=})

(defn ffilter
  [pred coll] (first (filter pred coll)))

(defn assoc-if-new [coll k v] (merge {k v} coll))

(defn map-dregs
  "Like map but when there is a different count between colls, applies input fn
  to the coll values until the biggest coll is empty."
  [f & colls]
  (lazy-seq
    (if-let [non-empty-colls (seq (filter seq colls))]
      (let [first-items (map first non-empty-colls)
            rest-colls (map rest non-empty-colls)]
        (cons (apply f first-items)
              (apply map-dregs f rest-colls))))))

(defn prepare-assertion-expression [expr]
  (let [conformed-expr (spec/conform ::eg-spec/expr-spec expr)]
    (if (= conformed-expr cross-invalid-spec-kw)
      (cross-throw (spec/explain-str ::eg-spec/expr-spec expr))
      (assoc-if-new (second conformed-expr) :expected 'boolean))))

(defn test?
  "Used to determine if function will be tested based on its focus state,
  and the focus state of the other function tests."
  [focus-metas focus?]
  (let [focuses (vals @focus-metas)
        focuses? (some true? focuses)]
    (boolean
      (or focus? (not focuses?)))))

(defn rm-lead-colon
  "Motivation: 'name' does not work for processed strings."
  [s] (if (= ":" (-> s first str))
        (subs s 1)
        s))

(defn cljs-safe-namespace
  "Like 'namespace', but occurrences of '.' are replaced with '-', to prevent compilation error in cljs.
  Used to create an unambiguous test name."
  [thing] (-> thing str (str/replace "." "-") symbol namespace rm-lead-colon))

(def ->test-name
  (comp symbol
        #(str % "-test")
        #(str/join "-" %)
        #(keep identity %)
        (juxt cljs-safe-namespace
              #(str (if (keyword? %) ":") (name %)))))

(defmacro ->example-test
  "Creates a clojure.test test for function being tested.
  Assertions are generated under the test using provided examples.
  Test name is derived from the fully qualified name of function under test, and by appending '-test' to it.
  Test may not run depending on its focus state, and of other function tests."
  [fn-sym examples focus-metas- focus?]
  (let [test-name (->test-name fn-sym)]
    `(let [test# (deftest ~test-name
                   (when (test? ~focus-metas- ~focus?)
                     ~@(map (fn [example]
                              (if (qualified-keyword? fn-sym)
                                (let [{:keys [spec-example spec-example-bang-one spec-example-bang-variadic]} example]
                                  (cond
                                    spec-example               `(is (valid-spec? ~fn-sym ~spec-example))
                                    spec-example-bang-one      `(is (invalid-spec? ~fn-sym ~(:any spec-example-bang-one)))
                                    spec-example-bang-variadic `(map #(is (invalid-spec? ~fn-sym %)) ~(:any spec-example-bang-variadic))))
                                (let [{:keys [params operator expected]} example
                                      equal? (= operator '=)
                                      ; in JVM CLJS, 'normalised-expected' code prevents: Caused by: clojure.lang.ExceptionInfo: Can't call nil
                                      normalised-expected (if (nil? expected) 'nil? expected)]
                                  `(cond
                                    ; changing assertion expression order of args may break reports 
                                    (and (fn? ~normalised-expected) (not ~equal?))                (is (pred-eg (~normalised-expected (~fn-sym ~@params))))
                                    (and (qualified-keyword? ~normalised-expected) (not ~equal?)) (is (valid-expected-spec? ~normalised-expected (~fn-sym ~@params)))
                                    :else                                                         (is (equal-eg? ~normalised-expected (~fn-sym ~@params)))))))
                            examples)))]
      ; passing down ^:focus meta to clj.test: see alter-test-var-update-fn
      ; FIXME not associng in cljs
      (alter-meta! (var ~test-name) #(assoc % :focus ~focus?))
      test#)))

(defmacro ->expression-test
  "Creates a clojure.test test for expression being tested.
  Assertion is generated under the test using provided expression.
  Test name is created as 'eg-test-<rand-id>'."
  [{:keys [expression operator expected]}]
  (let [rand-id (int (* (rand) 100000))
        test-name (symbol (str "eg-test-" rand-id))
        equal? (= operator '=)]
    `(deftest ~test-name
      ; in JVM CLJS, 'normalised-expected' code prevents: Caused by: clojure.lang.ExceptionInfo: Can't call nil
      ~(let [normalised-expected (if (nil? expected) 'nil? expected)]
        `(cond
          (or (and (fn? ~normalised-expected) (not ~equal?)) (nil? ~expected)) (is (pred-ex (~normalised-expected ~expression)))
          (and (qualified-keyword? ~normalised-expected) (not ~equal?))        (is (valid-expected-spec-ex? ~normalised-expected ~expression))
          :else                                                                (is (equal-ex? ~normalised-expected ~expression)))))))
        

(defn assoc-focus-metas
  "Creates a new entry in fn to focus? map for qualified function in params."
  [focus-metas- fn-meta fn-sym]
  (let [fn-ns-name (-> fn-meta :ns str)
        qualified-fn-kw (keyword (str fn-ns-name "/" fn-sym))
        focus? (:focus fn-meta)]
    (assoc focus-metas- qualified-fn-kw focus?)))

(defn alter-test-var-update-fn
  "Meant for use with 'alter-var-root' to decorate 'clj.test/test-var' with
  test check on focus state."
  [test-v]
  (fn [v]
    (let [focus? (-> v meta :focus)]
      (if (test? focus-metas focus?)
        (test-v v)))))

(defn named-dont-care? [thing]
  (and (symbol? thing)
       (= \$ (-> thing name first))))

(def dont-care? (some-fn #{'_} named-dont-care?))

(defn fill-dont-cares
  "Takes in example pairs, and fills every occurrence of a don't care with values from other examples.
  Each '_' don't care occurrence is replaced with a value from another example at the same args position.
  Each named don't care (prefixed with '$'), is replaced the same way as in '_', then propagated to every occurrence under
  its expected value."
  [examples]
  (let [params (map :params examples)
        choices-per-param (apply map-dregs #(->> %& (remove dont-care?) vec) params)
        fo (fn [{:keys [params operator expected] :as example}]
             ; OPTIMIZE to choose at random
             (let [fi (fn [{:keys [params operator expected] :as example} [param choices]]
                        (if (dont-care? param)
                          (if-let [choice (first choices)]
                            (let [pw-f #(if (= param %) choice %)]
                              {:params (concat params [choice])
                               :operator operator
                               :expected (if (named-dont-care? param)
                                           (postwalk pw-f expected)
                                           expected)})
                            (cross-throw (str "No choices found for don't care: " param)))
                          (update example :params concat [param])))]
               (reduce fi
                       (assoc example :params [])
                       (map #(vec %&) params choices-per-param))))]
    (map fo examples)))

(defn ensure-vec-wrapped-params [example]
  (update example :params #(if (vector? %) % [%])))

(defmulti prepare-examples (fn [[test-thing-type _]] test-thing-type))

(defmethod prepare-examples :function [[_ {:keys [examples]}]]
  (fill-dont-cares
    (map (comp ensure-vec-wrapped-params second) examples)))

(defmethod prepare-examples :spec [[_ {:keys [examples]}]]
  (map #(apply hash-map %) examples))

(defmacro ->eg-test
  "Common logic between 'eg' and 'ge'."
  [[test-thing & _ :as test-body] egge-spec]
  (let [conformed-examples (spec/conform egge-spec test-body)]
    (if (= conformed-examples cross-invalid-spec-kw)
      `(cross-throw (spec/explain-str ~egge-spec ~test-body))
      (let [fn-meta (meta test-thing)
            focus? (:focus fn-meta)
            examples (prepare-examples conformed-examples)]
        `(do (swap! focus-metas assoc-focus-metas ~fn-meta ~test-thing)
             (->example-test ~test-thing ~examples focus-metas ~focus?))))))

(defmacro eg
  "Test function using examples of parameters / expected value. See readme for usage."
  [& args] `(->eg-test ~args ::eg-spec/eg-test))

(defmacro ge
  "Test function using examples of parameters / expected value. See readme for usage."
  [& args] `(->eg-test ~args ::eg-spec/ge-test))

(defmacro ex
  "Test arbitrary expressions against corresponding expected values.
  See readme for usage."
  [& body]
  (let [parsed-test-expr (prepare-assertion-expression body)]
    `(->expression-test ~parsed-test-expr)))

#?(:clj
  ; TODO support alter-test-var-update-fn for cljs
  (alter-var-root (var clj.test/test-var) alter-test-var-update-fn))

#?(:clj
  (defn set-eg-no-refresh!
    "Interns 'eg', 'ge', and 'ex' in clojure.core, to be able to use those forms without requires in test ns."
    [egs]
    (let [eg-var (if (ffilter #{'eg} egs) (intern 'clojure.core (with-meta 'eg {:macro true}) @#'eg))
          ge-var (if (ffilter #{'ge} egs) (intern 'clojure.core (with-meta 'ge {:macro true}) @#'ge))
          ex-var (if (ffilter #{'ex} egs) (intern 'clojure.core (with-meta 'ex {:macro true}) @#'ex))]
      (set (keep identity [eg-var ge-var ex-var])))))

#?(:clj ; FIXME cannot be tested – calling clojure.tools.namespace.repl/refresh causes lein test to run 0 tests
  (defn set-eg!
    "Interns 'eg', 'ge', and 'ex' in clojure.core, to be able to use those forms without requires in test ns.
    Then, refreshes all namespaces for cases when a test namespace is required before this function is called."
    [& egs]
    (set-eg-no-refresh! egs)
    (clojure.tools.namespace.repl/refresh)))
