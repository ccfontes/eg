(ns eg.test.fixtures
  (:require [clojure.spec.alpha :as spec]))

(defn foo [x] inc)

(defn bar [x] inc)

(defn noargs [] "foo")

(defn js-eggs [x] x)

(spec/def ::string string?)

(spec/def ::int int?)

(spec/def ::map map?)
