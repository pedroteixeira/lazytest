(ns simple-pass
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add small numbers"
    (= 7 (+ 3 4))))
