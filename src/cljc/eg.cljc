(ns eg ^{:author "Carlos da Cunha Fontes"
         :license {:name "The Universal Permissive License (UPL), Version 1.0"
                   :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  #?(:cljs (:require-macros [eg :refer [eg ge ex]]))
  (:require [eg.platform :refer [deftest is cross-throw ->clj valid-spec? invalid-spec? equal?]]
            [eg.report] ; here for side-effects extending clj.test/assert-expr, cljs.test/assert-expr, and js/cljs.test$macros.assert_expr
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [clojure.test :as clj.test]
   #?(:cljs [cljs.test :include-macros true])
    #?(:clj [clojure.tools.namespace.repl])))

(defonce focus-metas (atom {}))

(def operators #{'=> '<= '=})

(defn ffilter
  [pred coll] (first (filter pred coll)))

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

(defn examples-acc
  "Accumulates examples, mainly taking into account operator used in example."
  [[parts part] token]
  (let [new-part (conj part token)]
    (if (or (empty? part) (operators token))
      [parts new-part]
      [(conj parts new-part) []])))

(defn variadic-bang?
  "Checks if token is a list with '!' as first element."
  [token]
  (and (list? token)
       (= (first token) '!)
       (> (count token) 1)))

(defn spec-eg-acc
  "Accumulates examples for a spec, mainly because some could appear negated using '!'."
  [[parts part] token]
  (let [new-part (conj part token)]
    (if (empty? part)
      (cond
        (= '! token)           [parts new-part]
        (variadic-bang? token) [(->> token rest (interpose '!) (cons '!) (partition 2) (concat parts)) []]
        :else                  [(conj parts new-part) []])
      [(conj parts new-part) []])))

(defn parse-example
  "Normalizes an 'eg/ge' example's operator, and order of function parameters vs expected result."
  [example ge?]
  (let [normalise-rev-ex #(juxt last (constantly %) first)
        normalise-ex #(juxt first (constantly %) last)
        parsed-ex
          (if (#{2 3} (count example))
            (if (= (second example) '=)
              ((normalise-ex '=) (if ge? (reverse example) example))
              (if (or (and ge? (not= (second example) '=>))
                      (= (second example) '<=))
                ((normalise-rev-ex '=>) example)
                ((normalise-ex '=>) example)))
            (let [egge (if ge? "ge" "eg")]
              (cross-throw (str egge " examples need to come in pairs, but found only: '" (first example) "'"))))
        params (first parsed-ex)
        normalized-params (if (vector? params) params [params])]
    (cons normalized-params (rest parsed-ex))))

(defn parse-expression
  "Normalizes an 'ex' expression's operator, and order of test expression vs expected result."
  [expr]
  (if (#{'=> '=} (second expr))
    expr
    (if (= (second expr) '<=)
      ((juxt last (constantly '=>) first) expr)
      (cross-throw (str "Was expecting an arrow, but found '" (second expr) "' instead..")))))

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
                                (let [example-val (last example)]
                                  (if (= (first example) '!)
                                    `(is (invalid-spec? ~fn-sym ~example-val))
                                    `(is (valid-spec? ~fn-sym ~example-val))))
                                (let [equal? (= (second example) '=)
                                      param-vec (first example)
                                      expected (last example)
                                      ; to avoid CompilerException on unreached branch: 'Can't call nil'
                                      normalised-expected (if (nil? expected) 'nil? expected)]
                                  `(cond
                                    (and (fn? ~normalised-expected) (not ~equal?)) (is (~normalised-expected (~fn-sym ~@param-vec)))
                                    (and (qualified-keyword? ~normalised-expected) (not ~equal?)) (is (valid-spec? ~normalised-expected (~fn-sym ~@param-vec)))
                                    :else (is (equal? ~normalised-expected (~fn-sym ~@param-vec)))))))
                            examples)))]
      ; passing down ^:focus meta to clj.test: see alter-test-var-update-fn
      ; FIXME not associng in cljs
      (alter-meta! (var ~test-name) #(assoc % :focus ~focus?))
      test#)))

(defmacro ->expression-test
  "Creates a clojure.test test for expressions being tested.
  Assertions are generated under the test using provided expressions.
  Test name is created as 'eg-test-<rand-id>'."
  [examples]
  (let [rand-id (int (* (rand) 100000))
        test-name (symbol (str "eg-test-" rand-id))]
    `(deftest ~test-name
      ~@(map (fn [[res op expected]]
               ; to avoid CompilerException on unreached branch: 'Can't call nil'
               (let [equal? (= op '=)
                     normalised-expected (if (nil? expected) 'nil? expected)]
                 `(if (and (fn? ~normalised-expected) (not ~equal?))
                   (is (~normalised-expected ~res))
                   (is (= (->clj ~normalised-expected) (->clj ~res))))))
             examples))))

(defn assoc-focus-metas
  "Creates a new entry in fn to focus? map for qualified function in params."
  [focus-metas- fn-meta fn-sym]
  (let [fn-ns-name (-> fn-meta :ns str)
        qualified-fn-kw (keyword (str fn-ns-name "/" fn-sym))
        focus? (:focus fn-meta)]
    (assoc focus-metas- qualified-fn-kw focus?)))

; FIXME function not executing in cljs
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
  (let [input-examples (map first examples)
        choices-per-param (apply map-dregs #(->> %& (remove dont-care?) vec) input-examples)
        fo (fn [example]
             ; OPTIMIZE to choose at random
             (let [params (first example)
                   op (operators (second example))
                   exp (last example)
                   fi (fn [[param-acc op- exp] [param choices]]
                        (if (dont-care? param)
                          (if-let [choice (first choices)]
                            (let [pw-f #(if (= param %) choice %)]
                              [(concat param-acc [choice])
                               op-
                               (if (named-dont-care? param) (postwalk pw-f exp) exp)])
                            (cross-throw (str "No choices found for don't care: " param)))
                          [(concat param-acc [param]) op- exp]))
                   ret-ex (reduce fi [[] op exp] (map #(vec %&) params choices-per-param))]
               (if (-> ret-ex second nil?) [(first ret-ex) (last ret-ex)] ret-ex)))]
    (map fo examples)))

(defn ->examples
  "Takes in an eg body, and returns example pairs."
  [test-thing ge? body]
  (cond
    (symbol? test-thing)
      (->> body
        (reduce examples-acc [[] []])
        (first)
        (map #(parse-example % ge?))
        (fill-dont-cares))
    (keyword? test-thing) (first (reduce spec-eg-acc [[] []] body))
    :else (cross-throw (str "Not a valid test name type: " test-thing))))

(defmacro eg-helper
  "Common logic between 'eg' and 'ge'."
  [[fn-sym & body] ge?]
  (let [examples (->examples fn-sym ge? body)
        fn-meta (meta fn-sym)
        focus? (:focus fn-meta)]
    `(do (swap! focus-metas assoc-focus-metas ~fn-meta ~fn-sym)
         (->example-test ~fn-sym ~examples focus-metas ~focus?))))

(defmacro eg
  "Test function using examples of parameters / expected value. See readme for usage."
  [& args] `(eg-helper ~args false))

(defmacro ge
  "Like 'eg' but example components are reversed. See readme for usage."
  [& args] `(eg-helper ~args true))

(defmacro ex
  "Test arbitrary expressions against corresponding expected values.
  See readme for usage."
  [& body]
  (let [examples (->> body (partition 3) (map parse-expression))]
    `(->expression-test ~examples)))

#?(:clj
  (alter-var-root (var clj.test/test-var) alter-test-var-update-fn))

#?(:cljs ; FIXME this is not redefining 'test-var'
  (set! cljs.test/test-var (alter-test-var-update-fn cljs.test/test-var)))

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
