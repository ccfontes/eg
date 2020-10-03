(ns eg.test.fixtures
  ; Alias below named 'fixtures-spec', rather than 'spec' as in other
  ; namespaces, in order to uncover potential missing require for
  ; 'clojure.spec.alpha' ns in integration tests
  (:require [clojure.spec.alpha :as fixtures-spec]))

(defn foo [x] inc)

(defn bar [x] inc)

(defn noargs [] "foo")

(defn js-eggs [x] x)

(fixtures-spec/def ::string string?)

(fixtures-spec/def ::int int?)

(fixtures-spec/def ::map map?)
