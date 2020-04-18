# Run eg's own tests

## Tests targeting Clojure

### Passing tests
```
lein clj-test-pass
```

### Failing tests
```
lein clj-test-fail
```

## Tests targeting ClojureScript JVM->nodejs
Install the following before proceeding, in order to get accurate test line
information on reports:
```
npm install source-map-support
```

### Passing tests
```
lein cljs-test-pass
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
Ran 12 tests containing 21 assertions.
19 failures, 0 errors.
Tests failed.
```