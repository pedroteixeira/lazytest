(ns simple-fail
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add small numbers"
    (= 10 (+ 3 4))))
