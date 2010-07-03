(ns skip-example-with-meta
  (:use lazytest.describe))

(describe + "with integers"
  ^{:skip true}
  (it "should add fives"
    (expect (= 1000 (+ 5 5))))
  (it "should add fours"
    (expect (= 8 (+ 4 4)))))
