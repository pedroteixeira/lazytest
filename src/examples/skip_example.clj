(ns skip-example
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add fives"
    :skip true
    (expect (= 1000 (+ 5 5))))
  (it "should add fours"
    (expect (= 8 (+ 4 4)))))
