(ns simple-pass
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add small numbers"
    (expect = 7 (+ 3 4))))
