# eg
[![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)
[![CircleCI](https://circleci.com/gh/ccfontes/eg.svg?style=svg)](https://circleci.com/gh/ccfontes/eg)
[![codecov](https://codecov.io/gh/ccfontes/eg/branch/master/graph/badge.svg)](https://codecov.io/gh/ccfontes/eg)

*eg* delivers `clojure.test` function tests with conciseness.

```clj
(deftest inc-test
  (is (= 1 (inc 0))))
```
in *eg* becomes:
```clj
(eg inc [0] 1)
```

Core ideas driving *eg*:
  - **conciseness** – spend less time reading and writing test boilerplate
  - **flexibility**:
    - switch order of examples to improve readability
    - check return against a predicate or equality relative to other data types
    - focus on specific tests while developing
  - **examples as data** - for trivial tool support, examples are just data!
  - **function like test definitions** - akin to `clojure.spec/fdef`, but for tests
  - **compatibility with clojure.test** - along with its excelent tooling support

*eg* targets both Clojure and ClojureScript JVM. Untested for ClojureScript JS.

## Installation
**Disclaimer:** *eg* is work-in-progress. Use it at your own risk!

**Leiningen/Boot**
```clj
[eg "0.2.3-alpha"]
```
**Clojure CLI/deps.edn**
```clj
eg {:mvn/version "0.2.3-alpha"}
```

## Usage
`eg` stands for *e.g.* (short for example), and `ge` is just `eg` reversed. Reversed example: `(ge inc 1 [0])`.

Let's try *eg*! Start by creating a REPL session and then requiring `eg` and `ge`:
```clj
(require '[eg :refer [eg ge]])
```

Each *eg* test tests one function using examples. You could think of it as a function's test definition:
```clj
  (eg not   ; testing clojure.core/not
    [false] ; with input parameters vector `[false]`
    true)   ; returning expected value `true`
```
a `clojure.test` test named `not-test` was generated.

There are times when we prefer to have expected values
on the left, and input parameters on the right.
For that we use `ge`, a useful mnemonic for the inverted flow of the test example:
```clj
  (ge + 10 [3 7]) ; one liners are also ok, as with `defn`
```

Each *eg* test can contain an arbitrary number of examples:
```clj
  (eg *
    [3]   3
    [3 2] 6)
```

Predicates can also be used in place of an expected value:
```clj
(eg dec [4] integer?)
```

`=>` or `<=` delimiters between input parameters and expected value can be used to improve readability, or
override the default order of `eg` or `ge`.
```clj
(eg hash-map
  [:d 1] {:d 1}
  [:a 1 :b 2 :c 3 :d 4] => {:a 1 :b 2 :c 3 :d 4}
  map? <= {:a 1 :b 2 :c 3 :d 4})
```

It's possible to run only selected tests by using metadata `^:focus` on `eg` or `ge`:
```clj
(eg ^:focus false? [false] true)
```
There are some caveats to consider when using `^:focus` **with ClojureScript**:
  1. The tests report counts towards non focused tests, although assertions under such tests are not executed.
  2. Assertions for tests defined directly with `clojure.test/deftest` will be executed, despite the presence of focused `eg`, or `ge` tests. 

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

## Run your tests
Finally, run your tests as you normally would with `clojure.test`.

**Clojure tests in the REPL:**
```clj
(clojure.test/run-all-tests)
; or
(clojure.test/run-tests some.ns)
```

**Clojure tests in the terminal:**
```
> lein test
```

**ClojureScript tests in the REPL:**
```clj
(cljs.test/run-all-tests)
; or
(cljs.test/run-tests some.ns)
```

## Roadmap
  1. Fix test name to support qualified symbols
  2. Document being able to skip a test with vanilla clojure
  3. Suffix test name with '-slow' when using ':slow' selector
  4. Mention:
     - leiningen `test-selectors` for use of metadata
     - https://github.com/weavejester/eftest
  5. Support expression testing
  6. Spec API macros `eg` and `ge`
  7. Test against ClojureScript JS
  8. Create API to access example data for i.e. tool use
  9. Document dev flow using clipboard
  10. Create focus of test partial example using don't care generator(s) for the rest
  11. Reduce clojure and clojurescript requirements
  12. Provide workaround to remove warning of eg being a single segment ns

## Run eg's own tests
Run tests expected to pass, targeting Clojure:
```clj
> lein clj-test-pass
```
Run tests expected to pass, targeting ClojureScript JVM->nodejs:
```clj
> lein cljs-test-pass
```
Run tests expected to fail, targeting Clojure:
```clj
> lein clj-test-fail
```
Run tests expected to fail, targeting ClojureScript JVM->nodejs:
```clj
> lein cljs-test-fail
```

## Software that works great with eg
  * [humane-test-output](https://github.com/pjstadig/humane-test-output) - Humane test output for clojure.test

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
