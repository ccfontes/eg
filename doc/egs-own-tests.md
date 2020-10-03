# Run eg's own tests

## Tests targeting Clojure

### Passing tests
Unit tests:
```
lein clj-test-pass-unit
```
Integration tests:
```
lein clj-test-pass-integration
```

### Failing tests
```
lein clj-test-fail
```

## Tests targeting ClojureScript JVM->nodejs
Do the following config before proceeding, in order to get accurate test line
information on reports.

First install source maps support node package:
```
npm install source-map-support
```
Enable source maps on your `project.clj` test build config:
```clj
{:cljsbuild
  {:builds
    {...
      {:compiler {:source-map true}}}}}
```

### Passing tests
Unit tests:
```
lein cljs-test-pass-unit
```
Integration tests:
```
lein cljs-test-pass-integration
```

### Failing tests
```
lein cljs-test-fail
```

## Passing tests targeting ClojureScript JS
```sh
lein tach planck
# or
lein tach lumo
```

## Failing tests notes
The test report should look like this:
```
Ran 15 tests containing 24 assertions.
22 failures, 0 errors.
Tests failed.
```
