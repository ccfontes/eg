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
                                 fn-identity-intercept
                                 valid-expected-spec?
                                 pred-ex]]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [clojure.test :as clj.test]
            [clojure.spec.alpha :as spec]
   #?(:cljs [cljs.test :include-macros true])
    #?(:clj [clojure.tools.namespace.repl])
   #?(:clj  [eg.report.clj])    ; here for side-effects to extend clojure.test/assert-expr
   #?(:clj  [eg.report.cljs])   ; here for side-effects to extend cljs.test/assert-expr
   #?(:cljs [eg.report.cljs]))) ; here for side-effects to extend cljs.test/report, and js/cljs.test$macros.assert_expr

(defonce focus-metas (atom {}))

(spec/def ::expr-spec (spec/or :one-arg (spec/tuple any?)
                               :two-arg (spec/tuple any? any?)
                               :three-args-normal (spec/tuple any? #{'= '=>} any?)
                               :three-args-inverted (spec/tuple any? #{'<=} any?)))

(def operators #{'=> '<= '=})

(defn ffilter
  [pred coll] (first (filter pred coll)))

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

(defmulti parse-expression
  "Normalizes an 'ex' expression's operator,
  and order of test expression vs expected result."
  (fn [expr]
    (let [conformed-expr (spec/conform ::expr-spec (vec expr))]
      (if (= conformed-expr cross-invalid-spec-kw)
        :invalid
        (first conformed-expr)))))

(defmethod parse-expression :one-arg [[truthy]] [(boolean truthy) '=> true])

(defmethod parse-expression :two-arg [[f l]] [l '=> f])

(defmethod parse-expression :three-args-normal [expr] expr)

(defmethod parse-expression :three-args-inverted [[f back-arrow l]] [l '=> f])

(defmethod parse-expression :invalid [expr]
  (cross-throw (apply str "Invalid expression: (ex "
                           (str/join (interpose " " expr)) ")")))

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
                                      ; in JVM CLJS, 'normalised-expected' code prevents: Caused by: clojure.lang.ExceptionInfo: Can't call nil
                                      normalised-expected (if (nil? expected) 'nil? expected)]
                                  `(cond
                                    ; changing assertion expression order of args may break reports 
                                    (and (fn? ~normalised-expected) (not ~equal?))                (is (fn-identity-intercept (~normalised-expected (~fn-sym ~@param-vec))))
                                    (and (qualified-keyword? ~normalised-expected) (not ~equal?)) (is (valid-expected-spec? ~normalised-expected (~fn-sym ~@param-vec)))
                                    :else                                                         (is (equal-eg? ~normalised-expected (~fn-sym ~@param-vec)))))))
                            examples)))]
      ; passing down ^:focus meta to clj.test: see alter-test-var-update-fn
      ; FIXME not associng in cljs
      (alter-meta! (var ~test-name) #(assoc % :focus ~focus?))
      test#)))

(defmacro ->expression-test
  "Creates a clojure.test test for expression being tested.
  Assertion is generated under the test using provided expression.
  Test name is created as 'eg-test-<rand-id>'."
  [res op expected]
  (let [rand-id (int (* (rand) 100000))
        test-name (symbol (str "eg-test-" rand-id))
        equal? (= op '=)]
    `(deftest ~test-name
      ; in JVM CLJS, 'normalised-expected' code prevents: Caused by: clojure.lang.ExceptionInfo: Can't call nil
      ~(let [normalised-expected (if (nil? expected) 'nil? expected)]
        `(if (or (and (fn? ~normalised-expected) (not ~equal?))
                 (nil? ~expected))
          (is (pred-ex (~normalised-expected ~res)))
          (is (equal-ex? ~normalised-expected ~res)))))))

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
  (let [example (parse-expression body)]
    `(->expression-test ~@example)))

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
