(ns simple-fail
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add small numbers"
    (expect = 10 (+ 3 4))))
