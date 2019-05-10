(ns eg.test.pass
  (:require
    [eg.platform :refer [deftest is testing]]
    [eg :refer [eg ge ->examples parse-examples]]))

(deftest ->examples-test
  (is (= '([[2] 1]) (->examples '([2] 1))))
  (is (= '([[2] => 1]) (->examples '([2] => 1))))
  (is (= '([[2] <= 1]) (->examples '([2] <= 1))))
  (is (= '([[2] => 1], [[1] 2])
         (->examples '([2] => 1, [1] 2)))))

(deftest parse-examples-test
  (testing "should be in order: input->output"
    (is (= '([[2] 1]) (parse-examples '([[2] 1]))))
    (is (= '([[2] 1]) (parse-examples '([[2] => 1]))))
    (is (= '([[2] 1]) (parse-examples '([1 <= [2]]))))))

(eg not [(not true)] true)

(ge * #(= 9 %) [3 3])

(eg -
  [1 2]       integer?
  [1 2]    => -1
  integer? <= [1 2])

(ge +
  3          [1 2]
  [1 2]    => integer?
  integer? <= [1 2])
