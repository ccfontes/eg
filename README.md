# eg
[![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)
[![CircleCI](https://circleci.com/gh/ccfontes/eg.svg?style=svg)](https://circleci.com/gh/ccfontes/eg)
[![codecov](https://codecov.io/gh/ccfontes/eg/branch/master/graph/badge.svg)](https://codecov.io/gh/ccfontes/eg)

eg delivers `clojure.test` function tests with conciseness.

e.g., `(eg inc [0] 1)` generates `clojure.test` boilerplate:
```clj
(deftest inc-test
  (is (= (inc 0) 1)))
```

The core ideas driving *eg* are:
  - conciseness – spend less time writing test boilerplate
  - flexibility - switch order of examples to improve readability
  - examples as data - for trivial tool support, it's just data!
  - function like test definitions - akin to `clojure.spec/fdef`, but for tests

*eg* targets both Clojure and ClojureScript JVM. Untested for ClojureScript JS.

## Install
**Disclaimer:** *eg* is work-in-progress. Use it at your own risk!

Leiningen/Boot
```
[eg "0.2.0-alpha"]
```
Clojure CLI/deps.edn
```
eg {:mvn/version "0.2.0-alpha"}
```
For Gradle or Maven install options, look here: [![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)

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

### Calling eg on same function multiple times
Between `eg`, and `ge`, choose the form that is most convenient for your combination of function examples and use it only once for testing a function. For example, **don't do this**:
```clj
(ge inc [1] 2)
(ge inc [0] 1)
```
or this:
```clj
(eg inc [1] 2)
(ge inc [0] 1)
```

### Test only functions
*eg* use is limited to testing functions. If you want to test macros or literals, `clojure.test` could be used for that purpose.

## Run your tests
Finally, run your tests as you normally would with `clojure.test`.

### Run your Clojure tests
in the REPL:
```clj
(clojure.test/run-all-tests)
; or
(clojure.test/run-tests some.ns)
```

or in the terminal:
```
lein test
```

### Run your ClojureScript tests in the REPL
```clj
(cljs.test/run-all-tests)
; or
(cljs.test/run-tests some.ns)
```

## Roadmap
  1. Add optional `=>` as in/out separator for readability between examples that are asymmetrical in length
  2. Spec API macros `eg` and `ge`
  3. Test against ClojureScript JS
  4. Create API to access example data for i.e. tool use
  5. Document dev flow using clipboard
  6. reduce clojure and clojurescript requirements
  7. Create extension interface for expected form

## Run eg's own tests
Run tests expected to pass, targeting Clojure:
```clj
lein clj-test-pass
```
Run tests expected to pass, targeting ClojureScript JVM->nodejs:
```clj
lein cljs-test-pass
```
Run tests expected to fail, targeting Clojure:
```clj
lein clj-test-fail
```
Run tests expected to fail, targeting ClojureScript JVM->nodejs:
```clj
lein cljs-test-fail
```

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
