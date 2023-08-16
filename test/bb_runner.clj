(ns bb-runner
  (:require
    [clojure.test :as test]
    [eg.test.pass.unit]
    [eg.test.fail]
    [eg.test.pass.integration]))

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
