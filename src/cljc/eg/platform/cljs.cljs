(ns eg.platform.cljs
  (:require [clojure.string :as str]
            [cljs.test]))

(defn ->file-and-line-repr [file line]
  (->> (if-not (exists? js/cljs.test$macros) (str ":" line))
    (str file)
    (list)))

(defn ->testing-fn-repr
  "Returns a string representation of the current function test.
  Renders function name as a list, then the source file and line of current assertion."
  [m] (let [{:keys [function file line]} m]
       (list (list function)
             (->file-and-line-repr file line))))

(defmethod cljs.test/report [:cljs.test/default :fail-spec]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [spec-kw example example-code reason expect-valid? file line]}]
  (let [example-code? (or (not= example example-code) (not expect-valid?))
        file-and-line (->file-and-line-repr file line)]
    (cljs.test/inc-report-counter! :fail)
    (println "\nFAIL in spec" (list spec-kw) file-and-line)
    (if example-code? (println "in example:" (if expect-valid? "" "!") example-code))
    (println (str (if example-code? "   ") "because:")
             (if expect-valid?
               (if (exists? js/cljs.test$macros)
                 reason ; TODO improve error msg for same quality as in clj, and cljs JVM
                 (->> (str/split reason #" spec: ") butlast (str/join " spec: ")))
               (str example " - is a valid example")))))

(defmethod cljs.test/report [:cljs.test/default :fail-equal]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [params expected actual file line] :as m}]
  (cljs.test/inc-report-counter! :fail)
  (apply println "\nFAIL in function" (->testing-fn-repr m))
  (println "      params: " params)
  (println "    expected: " expected)
  (println "      actual: " actual))
