#!/bin/sh

lein clj-test-pass-unit &&
lein clj-test-pass-integration | tee /dev/tty | (! grep -qE 'FAIL|ERROR|Error') &&
lein cljs-test-pass-unit | tee /dev/tty | (! grep -qE 'FAIL|ERROR|Error') &&
lein cljs-test-pass-integration &&
lein tach planck &&
lein clj-test-fail | tee /dev/tty | rg --quiet --multiline '24 assertions.*\n.*24 failures' &&
lein cljs-test-fail | tee /dev/tty | rg --quiet --multiline '24 assertions.*\n.*24 failures'
