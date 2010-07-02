(use '[lazytest.attach :only (groups)]
     '[lazytest.run :only (run-tests)]
     '[lazytest.results :only (success? container?)])

(println "Testing simple-pass ...")
(remove-ns 'simple-pass)
(load "src/examples/simple_pass")
(let [group-results (run-tests (first (groups 'simple-pass)))
      group-meta (meta (:source group-results))]
  (assert (success? group-results))
  (assert (container? group-results))
  (assert (string? (:doc group-meta)))
  (assert (re-matches #".*clojure\.core/\+ with integers.*" (:doc group-meta)))
  (assert (= 1 (count (:children group-results))))
  (let [example-result (first (:children group-results))
	example-meta (meta (:source example-result))]
    (assert (success? example-result))
    (assert (not (container? example-result)))
    (assert (string? (:doc example-meta)))
    (assert (re-matches #".*clojure\.core/\+ with integers should add small numbers.*"
			(:doc example-meta)))))

(println "Testing simple-fail ...")
(remove-ns 'simple-fail)
(load "src/examples/simple_fail")
(let [group-results (run-tests (first (groups 'simple-fail)))
      group-meta (meta (:source group-results))]
  (assert (not (success? group-results)))
  (let [example-result (first (:children group-results))
	example-meta (meta (:source example-result))]
    (assert (not (success? example-result)))))
