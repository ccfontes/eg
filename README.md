# eg [![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg) [![CircleCI](https://circleci.com/gh/ccfontes/eg.svg?style=svg)](https://circleci.com/gh/ccfontes/eg) [![codecov](https://codecov.io/gh/ccfontes/eg/branch/master/graph/badge.svg)](https://codecov.io/gh/ccfontes/eg) [![Universal Permissive License v1.0](https://img.shields.io/badge/license-UPL-%23f80000.svg)](LICENSE.md)

```clj
(deftest inc-test
  (is (= 1 (inc 0))))
```
in *eg* becomes:
```clj
(eg inc 0 1)
```

*eg* supports Clojure, ClojureScript JVM, and ClojureScript JS.

Vanilla clojure.test tests and *eg* test results are included in the same report.
Also, no custom test runner is required.

Check the [ideas driving eg](doc/ideas.md).

## Installation
**Disclaimer:** *eg* is becoming stable and there are no future plans on making breaking changes.
That said, please exercise care when upgrading *eg*.

**Leiningen/Boot**
```clj
[eg "0.4.10-alpha"]
```
**Clojure CLI/deps.edn**
```clj
eg {:mvn/version "0.4.10-alpha"}
```

## Usage
Let's try *eg*! Start by creating a REPL session and then requiring `eg` and `ge`:
```clj
(require '[eg :refer [eg ge]])
```
`eg` stands for *e.g.* (short for example), and `ge` is just `eg` reversed.

Each *eg* test tests one function using examples. You could think of it as a function's test definition:
```clj
  (eg not  ; testing clojure.core/not
    false  ; with input parameter `false`
    true)  ; returning expected value `true`
```
a `clojure.test` test named `not-test` was generated.

If a function under test takes multiple parameters, we need to wrap these with `[]`:
```clj
(eg * [3 2] 6)
```

When parameter is a vector, it needs to be wrapped with `[]` as well:
```clj
(eg set
  [[1 2]] #{1 2}) ; cannot do: [1 2] #{1 2}
```

Each *eg* test can contain an arbitrary number of examples:
```clj
  (eg *
    [3]   3
    [3 2] 6)
```

There are times when we prefer to have expected values
on the left, and input parameters on the right.
For that we use `ge`, a useful mnemonic for the inverted flow of the test example:
```clj
  (ge + 10 [3 7])
```

Expected values can be checked against a predicate or a spec:
```clj
(require '[clojure.spec.alpha :as spec])

(spec/def ::int integer?)

(eg dec
  [4] integer?
  [5] ::int)
```

`=>` or `<=` operators between input parameters and expected value can be used to improve readability, or
override the default example direction of `eg` or `ge`.
```clj
(eg hash-map
  [:d 1] {:d 1}
  [:a 1 :b 2 :c 3 :d 4] => {:a 1 :b 2 :c 3 :d 4})
```

`=>` or `<=` can be read as *is*. `{:a 1 :b 2 :c 3 :d 4}` *is* a `map?`:
```clj
(eg hash-map
  map? <= {:a 1}
  {:a 1} coll?) ; when no operator is used, 'is' operator semantics are assumed
```

`ex` makes it possible to test the result of calling an arbitrary form **ex**pression. Typical scenarios include testing the result of calling a macro (`eg`, and `ge` only support function testing), or decomposing the assertion of different properties or values from calling a form:
```clj
; let must be used outside of 'ex' when surrounding examples
(let [test-eg-ret (ex (inc 0) 1)
      f-len (count "eg-test-")]
  ; arrows are compulsory
  (ex var? <= test-eg-ret
      (-> test-eg-ret meta :test) => boolean
      (-> test-eg-ret meta :test) => fn?
      (-> test-eg-ret meta :name name (subs f-len)) => not-empty))
  ;=> eg-test-<rand-id>

  (ex (true? false) => false) ;=> eg-test-<rand-id>
```

If we want to check if the expected value is a function or a spec, the operator `=` is used:
```clj
(defn foo [] inc)

(eg foo [] = inc)
; or
(ge foo [] = 2)

(ex (foo 2) = inc)

(ex inc = (foo))

(ex (foo) = inc)

(defn bar [] ::inc)

(eg bar [] ::inc)
```

There are times when we just want to test a certain input parameter value, but fill the
remainder input parameters nevertheless. *eg* provides a *don't care* placeholder `_`,
for these cases:
```clj
(eg vector
  [1 2 3 4] [1 2 3 4]
  [5 6 _ 8] vector?
  [4 _ 5]   vector?)
```

We can arbitrarily name a *don't care* parameter by prefixing its name with `$`. A *named don't care* can also be bound with parts on the expected result. *Don't care* parameters are generated from parameters on the same argument position:
```clj
(eg assoc-in
  [{} [:a :b] {:eggs "boiled"}] => {:a {:b {:eggs "boiled"}}}
  [_ $spam _] => map?
  [_ _ $eggs] => {:a {:b $eggs}})
```
When writing examples, *don't cares* enable us to spend less time writting fillers, and the reader is able to better understand the focus
of the example.

Expecting arbitrarily nested JavaScript objects or arrays works out of the box:
```clj
(eg identity
  #js {:a [1]} => #js {:a [1]}
  #js {:a [1]} => (clj->js {:a [1]}))

(ex (identity #js {:a [1]}) => #js {:a [1]}))
```

Check if your specs are on the correct track using examples.
`eg` validates examples against a spec defined with a qualified keyword:
```clj
(require '[clojure.spec.alpha :as spec])

(spec/def ::string (spec/nilable string?))
(spec/def ::pos-int pos-int?)

(eg ::string nil "foo") ; `^:focus` cannot be used here at the moment
;=> <current-ns>-:string-test

(ge ::pos-int
  1
  ; test invalid examples, i.e., near a spec's boundary, using `!`
  ! 0
  (! -1 -2 -3)) ; equivalent to: ! -1 ! -2 ! -3
```

Quite often, writing tests becomes an afterthought, because creating test boilerblate like a new test namespace, requiring test forms and functions under test is too much of a hassle, while being immersed on writing code. It would make sense to have test forms globally available that we use almost as often as `defn`. Introducing `set-eg!` – call it at the development entrypoint of your program:
```clj
(require '[eg :refer [set-eg!]])
```
To use `eg`, `ge`, and `ex` in any namespace without additional requires, run:
```clj
(set-eg!)
;=> :reloading ()
```
To use `eg`, and `ge` in any namespace without additional requires, run:
```clj
(set-eg! 'eg 'ge)
```

PS - This functionality is only supported in Clojure.

It's possible to run only selected tests by using metadata `^:focus` on `eg` or `ge`:
```clj
(eg ^:focus false? [false] true)
```
There are some caveats to consider when using `^:focus` **with ClojureScript**:
  1. The tests report counts towards non focused tests, although examples under such tests are not executed.
  2. Assertions for tests defined directly with `clojure.test/deftest` will still be executed, despite the presence of focused `eg`, or `ge` tests. 

Skip running certain tests or examples using vanilla Clojure(Script) code:
```clj
#_(eg count '(9 8 7)
            3)

(eg count
 ; '(9 8 7) 3
 '(9 8 7 6) 4)
```

Between `eg`, and `ge`, choose the form that is most convenient for your combination of function examples and use it **only once** for testing a function. For example, **don't do this**:
```clj
(ge inc [1] 2)
(ge inc [0] 1)
```
**or this:**
```clj
(eg inc [1] 2)
(ge inc [0] 1)
```

When building with ClojureScript JVM, remove `WARNING: eg is a single segment namespace` warning with the compiler option `single-segment-namespace`:
```clj
{:cljsbuild
  {:builds
    [{:compiler
       {:warnings
         {:single-segment-namespace false}}}]}}
```

## Run your tests
Finally, run your tests as you normally would with `clojure.test`.

Clojure tests in the REPL:
```clj
(clojure.test/run-all-tests)
; or
(clojure.test/run-tests some.ns)
```

Clojure tests in the terminal:
```
> lein test
```

ClojureScript tests in the REPL:
```clj
(cljs.test/run-all-tests)
; or
(cljs.test/run-tests some.ns)
```

# Run eg's own tests
[Run eg's own tests](doc/egs-own-tests.md) expected to pass, fail, targeting Clojure, ClojureScript JVM, and ClojureScript JS.

## Test libraries which work great with eg
  * [eftest](https://github.com/weavejester/eftest) – Eftest is a fast and pretty Clojure test runner.
  * [humane-test-output](https://github.com/pjstadig/humane-test-output) – Humane test output for clojure.test

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
