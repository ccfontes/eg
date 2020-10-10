(ns eg.test.fixtures
  ; Alias below named 'fixtures-spec', rather than 'spec' as in other
  ; namespaces, in order to uncover potential missing require for
  ; 'clojure.spec.alpha' ns in integration tests
  (:require [clojure.spec.alpha :as fixtures-spec]
            [eg :refer [ex]]
            [clojure.test]))

(defn foo [x] inc)

(defn bar [x] inc)

(defn noargs [] "foo")

(defn js-eggs [x] x)

(fixtures-spec/def ::string string?)

(fixtures-spec/def ::int int?)

(fixtures-spec/def ::map map?)

(def exception-report
  (with-out-str
    (binding #?(:clj [clojure.test/*test-out* *out*]
                :cljs [])
      (clojure.test/test-var
        #?(:clj (ex (throw (Exception. "Oops!")) => string?)
           :cljs (ex (throw (js/Error. "Oops!")) => string?))))))
