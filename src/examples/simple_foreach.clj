(ns simple-foreach
  (:use lazytest.describe))

(describe + "with integers"
  (for-each "should add small numbers"
	    [a b sum] (expect (= sum (+ a b)))
	    [3 4 7]
	    [2 2 4]
	    [5 5 1000]))
