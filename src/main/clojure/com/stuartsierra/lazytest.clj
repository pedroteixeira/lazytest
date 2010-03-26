(ns com.stuartsierra.lazytest)

;;; PROTOCOLS

(defprotocol TestInvokable
  (invoke-test [t active]))

(defprotocol Successful
  (success? [r]))


;;; Results

(deftype TestResults [source children]
  Successful (success? [] (every? success? children)))

(deftype TestPassed [source states]
  Successful (success? [] true))

(deftype TestFailed [source states]
  Successful (success? [] false))

(deftype TestThrown [source states throwable]
  Successful (success? [] false))

(defn result-seq
  "Given a single TestResult, returns a depth-first sequence of that
  TestResult and all its children."
  [r]
  (tree-seq :children :children r))

(defn result-meta
  "Given a TestResult, returns the metadata map of its source."
  [r]
  (meta (:source r)))


;;; Contexts

(deftype Context [parents before after])

(defn- open-context
  "Opens context c, and all its parents, unless it is already active."
  [active c]
  (let [active (reduce open-context active (:parents c))
        states (map active (:parents c))]
    (if-let [f (:before c)]
      (assoc active c (or (active c) (apply f states)))
      active)))

(defn- close-context
  "Closes context c and removes it from active."
  [active c]
  (let [states (map active (:parents c))]
    (when-let [f (:after c)]
      (apply f (active c) states))
    (let [active (reduce close-context active (:parents c))]
      (dissoc active c))))

(defmacro defcontext
  "Defines a context.
  decl => docstring? [bindings*] before-body* after-fn?
  after-fn => :after [state] after-body*"
  [name & decl]
  (let [m {:name name, :ns *ns*, :file *file*, :line @Compiler/LINE}
        m (if (string? (first decl)) (assoc m :doc (first decl)) m)
        decl (if (string? (first decl)) (next decl) decl)
        bindings (first decl)
        bodies (next decl)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [pairs (partition 2 bindings)
          locals (vec (map first pairs))
          contexts (vec (map second pairs))
          before (take-while #(not= :after %) bodies)
          after (next (drop-while #(not= :after %) bodies))
          before-fn `(fn ~locals ~@before)]
      (when after (assert (vector? (first after))))
      (let [after-fn (when after
                       `(fn ~(vec (concat (first after) locals))
                          ~@after))]
        `(def ~name (Context ~contexts ~before-fn ~after-fn '~m nil))))))

(defn- has-after?
  "True if Context c or any of its parents has an :after function."
  [c]
  (or (:after c)
      (some has-after? (:parents c))))

(defmacro #^{:private true} try-contexts
  "Creates a try/finally block that opens contexts, stores their state
  in states, and finally closes contexts.  active is the current map
  of opened contexts."
  [active contexts states & body]
  `(let [active# ~active
         contexts# ~contexts
         merged# (reduce open-context active# contexts#)
         ~states (map merged# contexts#)]
     (try
      ~@body
      (finally
       (reduce close-context merged#
               ;; Only close contexts that weren't active at start:
               (filter #(not (contains? active# %))
                       (reverse contexts#)))))))

;;; Assertion types

(deftype SimpleAssertion [pred] :as this
  TestInvokable
    (invoke-test [active]
      (try
        (if (pred)
          (TestPassed this nil)
          (TestFailed this nil))
        (catch Throwable t
          (TestThrown this nil t)))))

(deftype ContextualAssertion [contexts pred] :as this
  TestInvokable
    (invoke-test [active]
      (try-contexts active contexts states
        (if (apply pred states)
          (TestPassed this states)
          (TestFailed this states))
        (catch Throwable t
          (TestThrown this states t)))))


;;; Container types

(deftype SimpleContainer [children] :as this
  clojure.lang.IFn
    (invoke [] (invoke-test this {}))
  TestInvokable
    (invoke-test [active]
      (try
       (TestResults this (map #(invoke-test % active) children))
       (catch Throwable t
         (TestThrown this nil t)))))

(deftype ContextualContainer [contexts children] :as this
  clojure.lang.IFn
    (invoke [] (invoke-test this {}))
  TestInvokable
    (invoke-test [active]
      (try-contexts active contexts states
        (let [results (map #(invoke-test % active) children)]
          ;; Force non-lazy evaluation when contexts need closing:
          (when (some has-after? contexts) (dorun results))
          (TestResults this results))
        (catch Throwable t
          (TestThrown this states t)))))


;;; Public API

(defmacro is
  "A series of assertions.  Each assertion is a simple expression,
  which will be compiled into a function.  A string will be attached
  as :doc metadata on the following assertion."
  [& assertions]
  (loop [r [], as assertions]
    (if (seq as)
      (let [[doc form nxt]
            (if (string? (first as))
              [(first as) (second as) (nnext as)]
              [nil (first as) (next as)])]
        (recur (conj r `(SimpleAssertion
                         (fn [] ~form)
                         {:doc ~doc,
                          :form '~form
                          :file *file*,
                          :line ~(:line (meta form))}
                         nil))
               nxt))
      `(SimpleContainer ~r {:generator 'is
                            :line ~(:line (meta &form))
                            :form '~&form}
                        nil))))

(defmacro given
  "A series of assertions using values from contexts.
  bindings is a vector of name-value pairs, like let, where each value
  is a context created with defcontext.  A string will be attached
  as :doc metadata on the following assertion."
  [bindings & assertions]
  (assert (vector? bindings))
  (assert (even? (count bindings)))
  (let [pairs (partition 2 bindings)
        locals (vec (map first pairs))
        contexts (vec (map second pairs))]
    (loop [r [], as assertions]
      (if (seq as)
        (let [[doc form nxt]
              (if (string? (first as))
                [(first as) (second as) (nnext as)]
                [nil (first as) (next as)])]
          (recur (conj r `(ContextualAssertion
                           ~contexts
                           (fn ~locals ~form)
                           {:doc ~doc,
                            :locals '~locals
                            :form '~form
                            :file *file*,
                            :line ~(:line (meta form))}
                           nil))
                 nxt))
        `(SimpleContainer ~r {:generator 'given
                              :line ~(:line (meta &form))
                              :form '~&form}
                          nil)))))

(defn- attributes
  "Reads optional name symbol and doc string from args,
  returns [m a] where m is a map containing keys
  [:name :doc :ns :file] and a is remaining arguments."
  [args]
  (let [m {:ns *ns*, :file *file*}
        m    (if (symbol? (first args)) (assoc m :name (first args)) m)
        args (if (symbol? (first args)) (next args) args)
        m    (if (string? (first args)) (assoc m :doc (first args)) m)
        args (if (string? (first args)) (next args) args)]
    [m args]))

(defn- options
  "Reads keyword-value pairs from args, returns [m a] where m is a map
  of keyword/value options and a is remaining arguments."
  [args]
  (loop [opts {}, as args]
    (if (and (seq as) (keyword? (first as)))
      (recur (assoc opts (first as) (second as)) (nnext as))
      [opts as])))

(defmacro testing
  "Creates a test container.
  decl   => name? docstring? option* child*

  name  => a symbol, will def a Var if provided.
  child => 'is' or 'given' or nested 'testing'.

  options => keyword/value pairs, recognized keys are:
    :contexts => vector of contexts to run only once for this container.
    :strategy => a test-running strategy."
  [& decl]
  (let [[m decl] (attributes decl)
        m (assoc m :line (:line (meta &form))
                 :generator `testing
                 :form &form)
        [opts decl] (options decl)
        {:keys [contexts strategy]} opts
        children (vec decl)
        sym (gensym "c")]
    `(let [~sym ~(if contexts
                    (do (assert (vector? contexts))
                        `(ContextualContainer ~contexts ~children '~m nil))
                    `(SimpleContainer ~children '~m nil))]
       ~(when (:name m) `(intern *ns* '~(:name m) ~sym))
       ~sym)))

(defmacro dotest
  "Creates an assertion function consisting of arbitrary code.
  Passes if it does not throw an exception.  Use assert for value
  tests.

  decl    => name? docstring? [binding*] body*
  binding => symbol context

  name  => a symbol, will def a Var if provided.
"
  [& decl]
  (let [[m decl] (attributes decl)
        m (assoc m :line (:line (meta &form))
                 :generator `dotest
                 :form &form)
        bindings (first decl)
        body (next decl)
        sym (gensym "c")]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [pairs (partition 2 bindings)
          locals (map first pairs)
          contexts (map second pairs)]
      `(let [~sym ~(if (seq contexts)
                     `(ContextualAssertion ~contexts (fn ~locals ~@body :ok)
                                           '~m nil)
                     `(SimpleAssertion (fn [] ~@body :ok) '~m nil))]
         ~(when (:name m) `(intern *ns* '~(:name m) ~sym))
         ~sym))))
