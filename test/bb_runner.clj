(ns bb-runner
  (:require
    [clojure.test :as test]
    [eg.test.pass.unit]
    [eg.test.fail]
    [eg.test.pass.integration]
    [babashka.deps]))

; source: https://jmglov.net/blog/2022-08-09-dogfooding-blambda-2.html
(alter-var-root #'babashka.deps/add-deps
  (fn [f]
    (fn [m]
      (println "[holy-lambda] Dependencies should not be added via add-deps. Move your dependencies to a layer!")
      (System/exit 1))))

(defn run-passing-tests []
  (test/run-tests
    'eg.test.pass.unit
    'eg.test.pass.integration))

(defn run-failing-tests []
  (test/run-tests 'eg.test.fail))

(defn -main []
  (let [{:keys [fail error]} (run-passing-tests)
        {:keys [pass]} (run-failing-tests)]
    (when (or (pos? (+ fail error)) (pos? pass))
      (System/exit 1))))
