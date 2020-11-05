(ns eg.spec ^{:author "Carlos da Cunha Fontes"
              :license {:name "The MIT License"
                        :url "https://github.com/ccfontes/eg/blob/master/LICENSE.md"}}
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::expr-spec (spec/or :one-arg (spec/tuple any?)
                               :two-arg (spec/tuple any? any?)
                               :three-args-normal (spec/tuple any? #{'= '=>} any?)
                               :three-args-inverted (spec/tuple any? #{'<=} any?)))

(spec/def ::example
  (spec/alt
    :explicit-straight-op (spec/cat :params any?
                                    :operator #{'= '=>}
                                    :expected any?)
    :explicit-inverted-arrow (spec/cat :expected any?
                                       :operator #{'<=}
                                       :params any?)
    :implicit-straight-arrow (spec/cat :params any?
                                       :expected any?)))

(spec/def ::rev-example
  (spec/alt
    :explicit-inverted-op (spec/cat :expected any?
                                    :operator #{'<= '=}
                                    :params any?)
    :explicit-straight-arrow (spec/cat :params any?
                                       :operator #{'=>}
                                       :expected any?)
    :implicit-inverted-arrow (spec/cat :expected any?
                                       :params any?)))

(spec/def ::spec-example
  (spec/alt :spec-example-bang-one (spec/cat :bang #{'!}
                                             :any any?)
            :spec-example-bang-variadic (spec/spec (spec/cat :bang #{'!}
                                                             :any (spec/+ any?)))
            :spec-example any?))

(spec/def ::eg-test
  (spec/alt
    :function (spec/cat :fn-name symbol?
                        :examples (spec/* ::example))
    :spec (spec/cat :spec-name keyword?
                    :examples (spec/* ::spec-example))))

(spec/def ::ge-test
  (spec/alt
    :function (spec/cat :fn-name symbol?
                        :examples (spec/* ::rev-example))
    :spec (spec/cat :spec-name keyword?
                    :examples (spec/* ::spec-example))))
