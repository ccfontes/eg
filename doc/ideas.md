# Ideas driving eg

  - **conciseness** – spend less time reading and writing test boilerplate
  - **function like test definitions** - akin to `clojure.spec/fdef`, but for tests
  - **flexibility**:
    - switch order of examples to improve readability
    - check return against a predicate or equality relative to other data types
    - specialized on function testing, but able to test arbitrary expressions as well
  - **focus**:
    - focus on specific tests while developing
    - understand the focus of an assertion by using input *don't cares*
  - **example inputs as data** - for trivial tool integration, its just data!
  - **reach**:
    - supports clojure.test - along with its excelent tooling support
    - supports Clojure, ClojureScript JVM, and ClojureScript JS