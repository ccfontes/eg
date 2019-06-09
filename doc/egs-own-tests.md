# Run eg's own tests

## Passing tests
Run tests expected to pass, targeting Clojure:
```clj
> lein clj-test-pass
```
Run tests expected to pass, targeting ClojureScript JVM->nodejs:
```clj
> lein cljs-test-pass
```
Run tests expected to pass, targeting ClojureScript JS:
```sh
> lein tach planck
# or
> lein tach lumo
```

## Failing tests
Run tests expected to fail, targeting Clojure:
```clj
> lein clj-test-fail
```
Run tests expected to fail, targeting ClojureScript JVM->nodejs:
```clj
> lein cljs-test-fail
```
The test report should look like this:
```
Ran 8 tests containing 12 assertions.
12 failures, 0 errors.
Tests failed.
```