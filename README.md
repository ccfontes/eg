# eg
[![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)
[![CircleCI](https://circleci.com/gh/ccfontes/eg.svg?style=svg)](https://circleci.com/gh/ccfontes/eg)
[![codecov](https://codecov.io/gh/ccfontes/eg/branch/master/graph/badge.svg)](https://codecov.io/gh/ccfontes/eg)

A maximal noise reduction for creating `clojure.test` tests when testing functions.

This code `(eg inc [0] 1)` generates:
```clj
(deftest inc-test
  (is (= (inc 0) 1)))
```

The core idea behind *eg* is examples as data for function like test definitions.

*eg* targets both Clojure and ClojureScript JVM. Untested for ClojureScript JS.

**Disclaimer:** *eg* is work-in-progress. Use it at your own risk!

## Install
Leiningen/Boot
```
[eg "0.1.0-alpha"]
```
Clojure CLI/deps.edn
```
eg {:mvn/version "0.1.0-alpha"}
```
For more install options, look here: [![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)

## Usage

*eg* uses `eg` or `ge` macros, to generate `deftest` tests of clojure.test.

`eg` stands for *e.g.* (short for example), and `ge` is just `eg` reversed. Reversed example: `(ge inc 1 [0])`.

Let's try *eg*! Start by creating a REPL session and then requiring `eg` and `ge`:
```clj
(require '[eg.core :refer [eg ge]])
```

Each *eg* test tests one function using examples. You could think of it as a function test definition:
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

## Run your tests
Finally, run your tests as you normally would with `clojure.test`.

### Run your Clojure tests
In the REPL:
```clj
(clojure.test/run-all-tests)
; or
(clojure.test/run-tests some.ns)
```

In the terminal:
```
lein test
```

### Run your ClojureScript tests
```clj
(cljs.test/run-all-tests)
; or
(cljs.test/run-tests some.ns)
```
## Caveats

### Calling eg on same function multiple times
Between `eg`, and `ge`, choose the form that is most convenient for your combination of function examples and use it only once for testing a function. For example, don't do this:
```clj
(ge inc [1] 2)
(ge inc [0] 1)
```
or this:
```clj
(eg inc [1] 2)
(ge inc [0] 1)
```

### eg's scope
*eg* use is limited to testing functions. If you want to test macros or literals, `clojure.test` could be used for that.

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
## Features on top of clojure.test
  * Intuitive one to one mapping between a function and a test definition
  * Examples as data for trivial tool integration
  * No repetition of function calls for multiple examples of the same function
  * No `is`, just data, in and out

## Roadmap
  1. Add optional `=>` as in/out separator for readability between examples that are asymmetrical in length
  2. Spec API macros `eg` and `ge`
  3. Test against ClojureScript JS
  4. Create API to access example data for i.e. tool use
  5. Document dev flow using clipboard

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
