# eg
[![Clojars Project](https://img.shields.io/clojars/v/eg.svg)](https://clojars.org/eg)
[![CircleCI](https://circleci.com/gh/ccfontes/eg.svg?style=svg)](https://circleci.com/gh/ccfontes/eg)

A maximal reduction of noise for tests using `clojure.test` as backend.

> (eg inc [0] 1)

The core ideas behind *eg* are examples as data, and function like test definitions.

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

Let's try *eg*! Start a REPL session and then require `eg` and `ge`:
```clj
=> (require [eg.core :refer [eg ge]])
```

Each *eg* test tests one function using examples. Think of it as a function test definition:
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

Finally, run your tests as you would normally do with `clojure.test`.

For Clojure in the REPL:
```clj
(clojure.test/run-all-tests)
; or
(clojure.test/run-tests some.ns)
```

For Clojure in the terminal:
```
lein test
```

For ClojureScript in the REPL:
```clj
(cljs.test/run-all-tests)
; or
(cljs.test/run-tests some.ns)
```

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

## Roadmap
  1. support regex return check
  2. add optional `=>` as in/out separator for readability on long in/out
  3. spec API macros
  4. test against ClojureScript JS
  5. create API to access example data
  6. document dev flow using clipboard

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
