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
  - **function like test definitions** - akin to `clojure.spec/fdef`, but for tests
  - **flexibility**:
    - switch order of examples to improve readability
    - check return against a predicate or equality relative to other data types
    - specialized on function testing, but able to test arbitrary expressions as well
  - **focus**:
    - focus on specific tests while developing
    - understand the focus of an assertion by using input *don't cares*
  - **examples as data** - for trivial tool support, examples are just data!
  - **reach**:
    - supports clojure.test - along with its excelent tooling support
    - supports Clojure, ClojureScript JVM, and ClojureScript JS

## Installation
**Disclaimer:** *eg* is work-in-progress. Use it at your own risk!

**Leiningen/Boot**
```clj
[eg "0.4.1-alpha"]
```
**Clojure CLI/deps.edn**
```clj
eg {:mvn/version "0.4.1-alpha"}
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

`ex` makes it possible to test the result of calling an arbitrary form **ex**pression. Typical scenarios include testing the result of calling a macro (`eg`, and `ge` only support function testing), or decomposing the assertion of different properties or values from calling a form:
```clj
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

There are times when we just want to test a certain input parameter value, but fill the
remainder input parameters nevertheless. *eg* provides a *don't care* placeholder `_`,
for these cases:
```clj
(eg vector
  [1 2 3 4] [1 2 3 4]
  [5 6 _ 8] vector?
  [4 _ 5]   vector?)
```

We can arbitrarily name a *don't care* parameter by prefixing its name with `$`. A *named don't care* can also be bound with parts on the expected result:
```clj
(eg assoc-in
  [{} [:a :b] {:eggs "boiled"}] => {:a {:b {:eggs "boiled"}}}
  [_ $spam _] => map?
  [_ _ $eggs] => {:a {:b $eggs}})
```
When writing the assertion, *don't cares* enable us to spend less time doing fillers, and the reader is able to better understand the focus
of the assertion.

As a personal experience, writing tests often becomes an afterthought, because creating test boilerblate like a new test namespace, requiring test forms and functions under test is too much of a hassle, while being immersed on writting code. It makes sense to have test forms globally available that we use almost as often as `defn`. Introducing `set-eg!`! Call it at the development entrypoint of your program:
```clj
(require '[eg :refer [set-eg!]])
(set-eg!)
;=> :reloading ()
;=> #{#'clojure.core/eg #'clojure.core/ex #'clojure.core/ge}
```
Now use `eg`, `ge`, and `ex` anywhere you want to create new tests!

PS - This functionality is only supported in Clojure.

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

Remove `WARNING: eg is a single segment namespace` warning with the compiler option `single-segment-namespace`:
```clj
{:cljsbuild
  {:builds
    [{:compiler
       {:warnings
         {:single-segment-namespace false}}}]}}
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
  1. Document being able to skip a test with vanilla clojure
  2. Support checkers in arbitrary places
  3. Support docstring for `ex`
  4. Suffix test name with '-slow' when using ':slow' selector
  5. Mention:
     - leiningen `test-selectors` for use of metadata
     - https://github.com/weavejester/eftest
  6. Spec API macros `eg`, `ge`, and `ex`
  7. Create API to access example data for i.e. tool use
  8. document clipboard dev flow
  9. Reduce Clojure and ClojureScript requirements
  10. Solve `^:focus` caveats in ClojureScript
  11. Adapt failed assertions report to *eg*'s data capture capability

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
Run tests expected to pass, targeting ClojureScript JS:
```sh
> lein tach planck
# or
> lein tach lumo
```

## Software that works great with eg
  * [humane-test-output](https://github.com/pjstadig/humane-test-output) - Humane test output for clojure.test

## [License](LICENSE.md)
Copyright (c) 2019 Carlos da Cunha Fontes

The Universal Permissive License (UPL), Version 1.0
