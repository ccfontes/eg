(ns eg.platform.cljs
  (:require [clojure.string :as str]
            [cljs.test]))

(defmethod cljs.test/report [:cljs.test/default :fail-spec]
  ; Source: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/test.cljs
  [{:keys [spec-kw example example-code reason expect-valid? file line]}]
  (let [example-code? (or (not= example example-code) (not expect-valid?))
        file-and-line (list (str file (if-not (exists? js/cljs.test$macros) (str ":" line))))]
    (cljs.test/inc-report-counter! :fail)
    (println "\nFAIL in spec" (list spec-kw) file-and-line)
    (if example-code? (println "in example:" (if expect-valid? "" "!") example-code))
    (println (str (if example-code? "   ") "because:")
             (if expect-valid?
               (if (exists? js/cljs.test$macros)
                 reason ; TODO improve error msg for same quality as in clj, and cljs JVM
                 (->> (str/split reason #" spec: ") butlast (str/join " spec: ")))
               (str example " - is a valid example")))))
