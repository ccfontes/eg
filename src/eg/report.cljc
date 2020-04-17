(ns eg.report
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [cljs.test :include-macros true]
            [clojure.test :as clj.test]
            [eg.platform :refer [if-cljs ->clj]]))

(defn explain-str
  [& args] (apply spec/explain-str args))

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
          (case (:type (update m :type #(if (#{:fail-spec :fail-equal} %) :fail %)))
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
              m (case (:type (update m :type #(if (#{:fail-spec :fail-equal} %) :fail %)))
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
  "Support customised spec examples in a test result and call report."
  [[f spec-kw example] expect-valid?]
  `(let [result# (~f ~spec-kw ~example)]
    (if result#
      (do-report {:type :pass})
      (do-report {:type          :fail-spec
                  :spec-kw       ~spec-kw
                  :example       ~example
                  :example-code  '~example
                  :expect-valid? ~expect-valid?
                  :reason        (explain-str ~spec-kw ~example)}))
    result#))

(defn do-equal-report
  "Support customised spec examples in a test result and call report."
  [equal expected [f & params :as actual] expression?]
  `(let [result# (~equal (->clj ~expected) (->clj ~actual))]
    (if result#
      (do-report {:type :pass})
      (do-report {:type     :fail-equal
                  :function '~f
                  :params   (vec '~params)
                  :expected ~expected
                  :actual   ~actual
                  :expression? ~expression?}))
    result#))

(defn print-report
  [{:keys [params expected actual expression?] :as m}]
  (if expression?
      (println "\nFAIL in expression" (->testing-fn-repr m))
      (do (println "\nFAIL in function" (->testing-fn-repr m))
          (println "      params: " (pr-str params))))
  (println "    expected: " (pr-str expected))
  (println "      actual: " (pr-str actual)))

#?(:clj
    (do
      ; defmethods for cljs JVM
      (defmethod cljs.test/assert-expr 'eg.platform/valid-spec?
        [_ _ form] (do-spec-report form true))
      
      (defmethod cljs.test/assert-expr 'eg.platform/invalid-spec?
        [_ _ form] (do-spec-report form false))
      
      (defmethod cljs.test/assert-expr 'eg.platform/equal?
        [_ _ form] (apply do-equal-report (concat form [false])))
      
      (defmethod cljs.test/assert-expr 'eg.platform/equal-ex?
        [_ _ form] (apply do-equal-report (concat form [true])))))
