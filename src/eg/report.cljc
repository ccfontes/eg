(ns eg.report ^{:author "Carlos da Cunha Fontes"
                :license {:name "The MIT License"
                          :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [clojure.string :as str]
            [cljs.test :include-macros true]
            [clojure.test :as clj.test]
            [eg.platform :refer [if-cljs ->clj explain-data]]))

(defn normalise-pred
  "Represent predicate as a string.
  If predicate is part of clj core, omit the namespace."
  [pred]
  (if (and (symbol? pred)
           (#{"clojure.core" "cljs.core"} (namespace pred)))
    (name pred)
    (str pred)))

(defn normalise-because-error
  [{:keys [pred val]}]
  {:pred (normalise-pred pred), :val (pr-str val)})

(defn spec->because-error
  "Prepare a spec error for a 'because' report error message."
  [{clj-problems :clojure.spec.alpha/problems
    cljs-problems :cljs.spec.alpha/problems}]
  (first (or clj-problems cljs-problems)))

(defn ->because-error-msg
  "Unify report behavior for failing spec tests expected to be valid."
  [because-error]
  (let [{:keys [pred val]} (normalise-because-error because-error)]
    (str val " fails: " pred)))

(defn spec-because
  "Unify report behavior for failing spec tests expected to be valid and invalid."
  [example because-error expect-valid?]
  (str "   because: "
       (if expect-valid?
         (->because-error-msg because-error)
         (str example " is a valid example"))))

#?(:clj
  (defn- stacktrace-file-and-line
    "Extract file, and line number information from stracktrace.
     Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj"
    [stacktrace]
    (if (seq stacktrace)
      (let [^StackTraceElement s (first stacktrace)]
        {:file (.getFileName s) :line (.getLineNumber s)})
      {:file nil :line nil})))

(defn rm-cljsjs-st-fname-prefix-fluff
  "Remove unnecessary (for reports) prefix found in a filename of a cljs js stacktrace."
  [fname]
  (->> (str/replace fname "file:///" "")
       (drop-while (partial not= \@))
       (rest)
       (str/join)))

#?(:cljs
  (defn file-and-line
    "Extract file, line, and maybe column number information from stracktrace.
     Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs"
    [exception depth]
    (let [if-cljs-js-then-normalise #(if (exists? js/cljs.test$macros) (rm-cljsjs-st-fname-prefix-fluff %) %)]
      (if-let [stack-element (and (string? (.-stack exception))
                                  (some-> (.-stack exception)
                                    (str/split-lines)
                                    (get depth)
                                    (if-cljs-js-then-normalise)
                                    (str/trim)))]
        (let [fname (cljs.test/js-filename stack-element)
              [line column] (cljs.test/js-line-and-column stack-element)
              [fname line column] (cljs.test/mapped-line-and-column fname line column)]
          {:file fname :line line :column column})
        {:file (.-fileName exception)
         :line (.-lineNumber exception)}))))

#?(:clj
  (defn do-report
    "Add file and line information to a test result and call report.
     If you are writing a custom assert-expr method, call this function
     to pass test results to report.
     Modified clj.test fn to convert :fail-spec into :fail, in order to get file:line.
     Source: https://github.com/clojure/clojure/blob/master/src/clj/clojure/test.clj"
    [m] (clj.test/report
          (case (:type (update m :type #(if (#{:fail-spec :fail-default} %) :fail %)))
            :fail (merge (stacktrace-file-and-line (drop-while
                                                     #(let [classname (.getClassName ^StackTraceElement %)
                                                            classname-blacklist ["eg.report$" "eg.platform$" "java.lang." "clojure.test$" "clojure.core$ex_info"]]
                                                        (first (filter (partial str/starts-with? classname) classname-blacklist)))
                                                     (.getStackTrace (Thread/currentThread))))
                         m)
            :error (merge (stacktrace-file-and-line (.getStackTrace ^Throwable (:actual m))) m)
            m))))

#?(:cljs
  (defn do-report
    "Add file and line information to a test result and call report.
    If you are writing a custom assert-expr method, call this function to pass test results to report.
    Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs"
    [m] (let [st-depth (if (exists? js/cljs.test$macros) 2 3)
              m (case (:type (update m :type #(if (#{:fail-spec :fail-default} %) :fail %)))
                  :fail (merge (file-and-line (js/Error.) st-depth) m)
                  :error (merge (file-and-line (:actual m) 0) m)
                  m)]
          (cljs.test/report m))))

(defn ->file-and-line-repr
  "Takes path to a test file, and test line.
  Returns a list of path to a test file and test line concatenated as a string.
  If runtime is cljs JS, test line is not concatenated to path to test file."
  [file line]
  (->> (if-not (if-cljs (exists? js/cljs.test$macros)) (str ":" line))
    (str file)
    (list)))

(defn ->testing-fn-repr
  "Returns a heading representation of the current test."
  [{:keys [function file line expression?]}]
  (if expression?
    (->file-and-line-repr file line)
    (list (list function)
          (->file-and-line-repr file line))))

(defn do-spec-report
  "Call do-report with a prepared params map for a spec example."
  [[f spec-kw example] expect-valid?]
  `(let [result# (~f ~spec-kw ~example)]
    (if result#
      (do-report {:type :pass})
      (do-report {:type            :fail-spec
                  :spec-kw         ~spec-kw
                  :example         ~example
                  :example-code    '~example
                  :expect-valid?   ~expect-valid?
                  :spec-error-data (explain-data ~spec-kw ~example)}))
    result#))

(defn do-example-pred-report
  "Call do-report with a prepared params map for a function example taking a predicate checker."
  [[_ [pred actual :as result]] expression?]
  `(let [result# ~result]
    (if result#
      (do-report {:type :pass})
      (do-report {:type        :fail-default
                  :pred        '~pred
                  :actual      ~actual
                  :expression? ~expression?}))
    result#))

(defn do-pred-report
  "Call do-report with a prepared params map for a function example taking a predicate checker."
  [[_ [pred [f & params :as actual] :as result]] expression?]
  `(let [result# ~result]
    (if result#
      (do-report {:type :pass})
      (do-report {:type        :fail-default
                  :function    '~f
                  :pred        '~pred
                  :actual      ~actual
                  :params      (vec '~params)
                  :expression? ~expression?}))
    result#))

(defn do-expected-spec-report
  "Call do-report with a prepared params map for a function example taking a spec checker."
  [[_ spec-kw [f & params :as actual] :as result] expression?]
  `(let [result# ~result]
    (if result#
      (do-report {:type :pass})
      (do-report {:type            :fail-default
                  :function        '~f
                  :spec-kw         '~spec-kw
                  :actual          ~actual
                  :params          (vec '~params)
                  :expression?     ~expression?
                  :spec-error-data (explain-data ~spec-kw ~actual)}))
    result#))

(defn do-default-report
  "Call do-report with a prepared params map for a function example."
  [[equal expected [f & params :as actual]] expression?]
  `(let [result# (~equal (->clj ~expected) (->clj ~actual))]
    (if result#
      (do-report {:type :pass})
      (do-report {:type        :fail-default
                  :function    '~f
                  :params      (vec '~params)
                  :expected    ~expected
                  :actual      ~actual
                  :expression? ~expression?}))
    result#))

(defn print-report
  [{:keys [params expected actual expression? pred spec-kw spec-error-data] :as m}]
  (if expression?
    (println "\nFAIL in expression" (->testing-fn-repr m))
    (do (apply println "\nFAIL in function" (->testing-fn-repr m))
        (println "    params:" (pr-str params))))
  (cond
    pred    (println (spec-because nil {:pred pred, :val actual} true))
    spec-kw (println (spec-because nil (spec->because-error spec-error-data) true))
    :else   (do (println "  expected:" (pr-str expected))
                (println "    actual:" (pr-str actual)))))

#?(:clj
    (do
      ; defmethods for cljs JVM
      (defmethod cljs.test/assert-expr 'eg.platform/valid-spec?
        [_ _ assert-expr] (do-spec-report assert-expr true))
      
      (defmethod cljs.test/assert-expr 'eg.platform/invalid-spec?
        [_ _ assert-expr] (do-spec-report assert-expr false))
      
      (defmethod cljs.test/assert-expr 'eg.platform/equal?
        [_ _ assert-expr] (do-default-report assert-expr false))
      
      (defmethod cljs.test/assert-expr 'eg.platform/equal-ex?
        [_ _ assert-expr] (do-default-report assert-expr true))
        
      (defmethod cljs.test/assert-expr 'eg.platform/fn-identity-intercept
        [_ _ assert-expr] (do-pred-report assert-expr false))

      (defmethod cljs.test/assert-expr 'eg.platform/valid-expected-spec?
        [_ _ assert-expr] (do-expected-spec-report assert-expr false))

      (defmethod cljs.test/assert-expr 'eg.platform/pred-ex
        [_ _ assert-expr] (do-example-pred-report assert-expr true))))
