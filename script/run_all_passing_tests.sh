#!/bin/sh

lein clj-test-pass-unit &&
lein clj-test-pass-integration | tee /dev/tty | (! grep -qE 'FAIL|ERROR|Error') &&
lein cljs-test-pass-unit | tee /dev/tty | (! grep -qE 'FAIL|ERROR|Error') &&
lein cljs-test-pass-integration &&
lein tach planck
