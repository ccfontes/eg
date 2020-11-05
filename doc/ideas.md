# Ideas driving eg

  - **Conciseness:** spend less time reading and writing test boilerplate
  - **Function like test definitions:** akin to `clojure.spec/fdef`, but for tests
  - **Flexibility:**
    - switch order of examples to improve readability
    - check return against a predicate, a spec, or equality to other data types
    - specialized on function and spec testing, but able to test arbitrary expressions as well
  - **Focus:**
    - focus on specific tests while developing
    - understand the focus of an assertion by using input *don't cares*
  - **Example inputs and outputs as data:** for trivial tool integration, it's just data!
  - **Reach:**
    - Leverage clojure.test tools by using and extending clojure.test
    - supports Clojure, ClojureScript JVM, and ClojureScript JS