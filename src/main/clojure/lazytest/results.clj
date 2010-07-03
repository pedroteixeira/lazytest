(ns lazytest.results
  "Any object encapsulating results of a single test must implement
  the TestResult protocol.  The record types defined here should be
  created with the constructor functions pass, fail, throw, skip, and
  pending."
  (:use [lazytest.arguments :only (nil-or)]))

;;; PROTOCOLS

(defprotocol TestResult
  (success? [r] "True if this result and all its children passed."))

;;; TEST RESULT TYPES

(defrecord TestResultContainer [source children]
  TestResult
    (success? [this] (every? success? children)))

(defn container [source children]
  (TestResultContainer. source children))

(defn container? [x]
  (instance? TestResultContainer x))

(defrecord TestPassed [source states]
  TestResult
    (success? [this] true))

(defn pass [source states]
  (TestPassed. source states))

(defrecord TestFailed [source states reason]
  TestResult
    (success? [this] false))

(defn fail [source states reason]
  (TestFailed. source states reason))

(defrecord TestThrown [source states throwable]
  TestResult
    (success? [this] false))

(defn thrown [source states throwable]
  {:pre [(instance? Throwable throwable)]}
  (TestThrown. source states throwable))

(defn error? [x]
  (instance? TestThrown x))

(defrecord TestSkipped [source reason]
  TestResult
    (success? [this] true))

(defn skip
  ([source] (skip source nil))
  ([source reason]
     {:pre [(nil-or string? reason)]}
     (TestSkipped. source reason)))

(defn skipped? [x]
  (instance? TestSkipped x))

(defrecord TestPending [source reason]
  TestResult
    (success? [this] true))

(defn pending
  ([source] (pending source nil))
  ([source reason]
     {:pre [(nil-or string? reason)]}
     (TestPending. source reason)))

(defn pending? [x]
  (instance? TestPending x))

;;; FAILURE REASON TYPES

(defrecord NotEqualFailure [expected actual])

(defn not-equal [expected actual]
  (NotEqualFailure. expected actual))
