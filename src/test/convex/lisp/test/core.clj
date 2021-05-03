(ns convex.lisp.test.core

  "Testing Convex Core by generating Convex Lisp forms as Clojure data, converting them to source,
   and evaling."

  {:author "Adam Helinski"}

  (:require [clojure.test                    :as t]
            [clojure.test.check.properties   :as tc.prop]
            [clojure.test.check.clojure-test :as tc.ct]
            [convex.lisp                     :as $]
            [convex.lisp.hex                 :as $.hex]
            [convex.lisp.test.util           :as $.test.util]))


;;;;;;;;;;


(def max-size-coll

  ""

  5)


;;;;;;;;;;



(defn prop-clojure

  ""

  [core-symbol schema f-related]

  (tc.prop/for-all* [($.test.util/generator schema)]
                    (fn [x]
                      ($.test.util/eq (apply f-related
                                             x)
                                      ($.test.util/eval (list* core-symbol
                                                               x))))))



(defn prop-compare

  ""

  [core-symbol f-related]

  (prop-clojure core-symbol
                [:vector
                 {:min 1}
                 :convex/number]
                f-related))



(defn prop-double

  ""

  [core-symbol]

  (tc.prop/for-all* [($.test.util/generator [:vector
                                             {:min 1}
                                             :convex/number])]
                    (fn [x]
                      (double? ($.test.util/eval (list* core-symbol
                                                        x))))))



(defn prop-numeric

  ""

  [core-symbol]

  (tc.prop/for-all* [($.test.util/generator [:vector
                                             {:min 1}
                                             :convex/long])]
                    (fn [x]
                      ($.test.util/prop+

                        "Numerical computation of longs must result in a long"
                        (int? ($.test.util/eval (list* core-symbol
                                                       x)))

                        "Numerical computation with at least one double must result in a double"
                        (double? ($.test.util/eval (list* core-symbol
                                                          (update x
                                                                  (rand-int (dec (count x)))
                                                                  double))))))))



(defn prop-pred-data-false

  ""


  ([core-symbol schema-without]

   (prop-pred-data-false core-symbol
                         schema-without
                         nil))


  ([core-symbol schema-without clojure-pred]

   (tc.prop/for-all* [($.test.util/generator-data-without schema-without)]
                     (if clojure-pred
                       (fn [x]
                         (let [x-2 ($.test.util/eval-pred core-symbol
                                                          x)]
                           ($.test.util/prop+

                             "Always returns false"
                             (not x-2)

                             "Consistent with Clojure"
                             (= x-2
                                (clojure-pred x)))))
                       (fn [x]
                         (not ($.test.util/eval-pred core-symbol
                                                     x)))))))



(defn prop-pred-data-true

  ""


  ([core-symbol schema]

   (prop-pred-data-true core-symbol
                        schema
                        nil))


  ([core-symbol schema clojure-pred]

   (tc.prop/for-all* [($.test.util/generator schema)]
                     (if clojure-pred
                       (fn [x]
                         (let [x-2 ($.test.util/eval-pred core-symbol
                                                          x)]
                           ($.test.util/prop+

                             "Always returns true"
                             x-2

                             "Consistent with Clojure"
                             (= x-2
                                (clojure-pred x)))))
                       (fn [x]
                         ($.test.util/eval-pred core-symbol
                                                x))))))



;;;;;;;;;;


(tc.ct/defspec *--

  (prop-numeric '*))



(tc.ct/defspec +--

  (prop-numeric '+))



(tc.ct/defspec ---

  (prop-numeric '-))



(tc.ct/defspec div--

  (prop-double '/))



(tc.ct/defspec <--

  (prop-compare '<
                <))



(tc.ct/defspec <=--

  (prop-compare '<=
                <=))



(tc.ct/defspec =--

  (prop-compare '=
                =))



(tc.ct/defspec >=--

  (prop-compare '>=
                >=))



(tc.ct/defspec >--

  (prop-compare '>
                >))



(tc.ct/defspec abs--

  (tc.prop/for-all* [($.test.util/generator :convex/number)]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'abs
                                                        x))]
                        ($.test.util/prop+

                          "Must be positive"
                          (>= x-2
                              0)

                          "Type is preserved"
                          (= (type x-2)
                             (type x)))))))



(tc.ct/defspec -account-inexistant

  (tc.prop/for-all* [($.test.util/generator [:and
                                             :int
                                             [:>= 50]])]
                      (fn [x]
                        ($.test.util/prop+

                          "Account does not exist"
                          (false? ($.test.util/eval (list 'account?
                                                          x)))

                          "Actor does not exist"
                          (false? ($.test.util/eval (list 'actor?
                                                          x)))))))




(tc.ct/defspec address?--true

  (tc.prop/for-all* [($.test.util/generator :convex/address)]
                    (fn [x]
                      ($.test.util/eval (list 'address?
                                              x)))))



(tc.ct/defspec address?--false

  ;; TODO. Also test `actor?`? See #74.

  {:max-size max-size-coll}

  (tc.prop/for-all* [($.test.util/generator-data-without #{:convex/address
                                                           :convex/boolean  ;; TODO. See #73
                                                           :convex/char     ;; TODO. See #68
                                                           :convex/double
                                                           :convex/long})]
                    (fn [x]
                      (false? ($.test.util/eval ($/templ {'?x x}
                                                         '(address? (quote ?x))))))))



(tc.ct/defspec blob?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'blob?
                        #{:convex/blob}))



(tc.ct/defspec blob?--true

  (prop-pred-data-true 'blob?
                       :convex/blob))



(tc.ct/defspec boolean?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'boolean?
                        #{:convex/boolean}
                        boolean?))



(t/deftest boolean?--true

  (t/is (true? ($.test.util/eval true))
        "True")

  (t/is (false? ($.test.util/eval false))
        "False"))



(tc.ct/defspec byte--

  (tc.prop/for-all* [($.test.util/generator :convex/number)]
                    (fn [x]
                      (<= Byte/MIN_VALUE
                          ($.test.util/eval (list 'byte
                                                  x))
                          Byte/MAX_VALUE))))



(tc.ct/defspec ceil--

  (prop-clojure 'ceil
                [:tuple :convex/number]
                #(StrictMath/ceil %)))



(tc.ct/defspec coll?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'coll?
                        #{:convex/list
                          :convex/map
                          :convex/set
                          :convex/vector}
                        coll?))



(tc.ct/defspec coll?--true

  {:max-size max-size-coll}

  (prop-pred-data-true 'coll?
                       :convex/collection
                       coll?))



(tc.ct/defspec create-account

  (tc.prop/for-all* [($.test.util/generator :convex/hexstring-32)]
                    (fn [x]
                      (let [ctx ($.test.util/eval-context ($/templ {'?hexstring x}
                                                                   '(def addr
                                                                         (create-account ?hexstring))))]
                        ($.test.util/prop+

                          "Address is interned"
                          ($.test.util/valid? :convex/address
                                              ($.test.util/eval ctx
                                                                'addr))

                          "Valid account"
                          ($.test.util/eval ctx
                                            '(account? addr))

                          "Not an actor"
                          (not ($.test.util/eval ctx
                                                 '(actor? addr)))

                          "Result is an address"
                          ($.test.util/eval ctx
                                            '(address? addr))

                          "Balance is 0"
                          (zero? ($.test.util/eval ctx
                                                   '(balance addr)))
                          )))))



(tc.ct/defspec dec--double

  ;; Unintuitive behavior. When sufficiently small double, is cast to 0.
  ;; Not small enough, get cast to `Long/MIN_VALUE` and underflows.

  (tc.prop/for-all* [($.test.util/generator [:double
                                             {:min (double Long/MIN_VALUE)}])]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'dec
                                                        x))]
                        ($.test.util/prop+

                          "Result is always a long"
                          (int? x-2)

                          "Decrement higher than maximum long"
                          (if (>= x
                                  Long/MAX_VALUE)
                            (= x-2
                               (dec Long/MAX_VALUE))
                            true)

                          "Decrement in long range"

                          (if (< Long/MIN_VALUE
                                 x
                                 Long/MAX_VALUE)
                            (= x-2
                               (dec (long x)))
                            true))))))




(t/deftest dec--double-underflow

  (t/is (= Long/MAX_VALUE
           ($.test.util/eval (list 'dec
                                   (double Long/MIN_VALUE))))))



(tc.ct/defspec dec--long

  (tc.prop/for-all* [($.test.util/generator :convex/long)]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'dec
                                                        x))]
                        ($.test.util/prop+

                          "Result is always a long"
                          (int? x-2)

                          "Decrement or underflow"
                          (= x-2
                             (if (= x
                                    Long/MIN_VALUE)
                               Long/MAX_VALUE
                               (dec x))))))))



(tc.ct/defspec exp--

  (prop-clojure 'exp
                [:tuple :convex/number]
                #(StrictMath/exp %)))



(tc.ct/defspec floor--

  (prop-clojure 'floor
                [:tuple :convex/number]
                #(StrictMath/floor %)))



(defn -eval-fn?

  "Returns true if the given form evals to a function."

  [form]

  ($.test.util/eval (list 'fn?
                          form)))



(defn -eval-fn

  "Returns true if calling the given function `form` with the given `arg+` returns `ret`."

  [form arg+ ret]

  ($.test.util/eval (list '=
                          ret
                          (list* form
                                 arg+))))


(defn -eval-fn-def

  "Like [[-eval-fn]] but interns the function first using `def`.

   Also tests `fn?`."
  

  [form arg+ ret]

  ($.test.util/result+ ($.test.util/eval ($/templ {'?call (list* 'f
                                                                 arg+)
                                                   '?fn   form
                                                   '?ret  ret}
                                                  '(do
                                                     (def f
                                                          ?fn)
                                                     [(fn? f)
                                                      (= ?ret
                                                         ?call)])))
                       ["Fn?"
                        "Equal"]))



(defn -eval-fn-let

  "Like [[-eval-fn]] but binds the function locally using `let`.

   Also tests `fn?`."

  [form arg+ ret]

  ($.test.util/result+ ($.test.util/eval ($/templ {'?call (list* 'f
                                                                 arg+)
                                                   '?fn   form
                                                   '?ret  ret}
                                                  '(let [f ?fn]
                                                     [(fn? f)
                                                      (= ?ret
                                                         ?call)])))
                       ["Fn?"
                        "Equal"]))



(tc.ct/defspec fn--arg-0

  ;; Calling no-arg functions.

  {:max-size max-size-coll}

  (tc.prop/for-all* [($.test.util/generator :convex/data)]
                    (fn [x]
                      (let [x-2     (list 'quote
                                          x)
                            fn-form (list 'fn
                                          []
                                          x-2)
                            templ   (fn [form]
                                      ($/templ {'?fn fn-form
                                                '?x  x-2}
                                               form))]
                        ($.test.util/prop+

                          "Function?"
                          (-eval-fn? fn-form)

                          "Direct"
                          (-eval-fn fn-form
                                    nil
                                    x-2)
  
                          "Interned"
                          (-eval-fn-def fn-form
                                        nil
                                        x-2)
  
                          "Let"
                          (-eval-fn-let fn-form
                                        nil
                                        x-2))))))



(tc.ct/defspec fn--arg-fixed

  ;; Calling functions with a fixed number of arguments.

  {:max-size max-size-coll}

  (tc.prop/for-all* [($.test.util/generator-binding+ 1)]
                    (fn [x]
                      (let [arg+     (mapv #(list 'quote
                                                  (second %))
                                           x)
                            binding+ (mapv first
                                           x)
                            fn-form  (list 'fn
                                           binding+
                                           binding+)]
                        ($.test.util/prop+

                          "Function?"
                          (-eval-fn? fn-form)

                          "Direct"
                          (-eval-fn fn-form
                                    arg+
                                    arg+)

                          "Interned"
                          (-eval-fn-def fn-form
                                        arg+
                                        arg+)

                          "Let"
                          (-eval-fn-let fn-form
                                        arg+
                                        arg+))))))



(tc.ct/defspec fn--arg-variadic

  ;; Calling functions with a variadic number of arguments.

  {:max-size max-size-coll}

  (tc.prop/for-all* [($.test.util/generator-binding+ 1)]
                    (fn [x]
                      (let [arg+       (mapv #(list 'quote
                                                    (second %))
                                             x)
                            binding+   (mapv first
                                             x)
                            pos-amper  (rand-int (count binding+))
                            binding-2+ (vec (concat (take pos-amper
                                                          binding+)
                                                    ['&]
                                                    (drop pos-amper
                                                          binding+)))
                            fn-form    (list 'fn
                                             binding-2+
                                             binding+)
                            ret        (update arg+
                                               pos-amper
                                               vector)]
                        ($.test.util/prop+

                          "Function?"
                          (-eval-fn? fn-form)

                          "Direct"
                          (-eval-fn fn-form
                                    arg+
                                    ret)

                          "Interned"
                          (-eval-fn-def fn-form
                                        arg+
                                        ret)


                          "Let"
                          (-eval-fn-let fn-form
                                        arg+
                                        ret)

                          "1 argument less"
                          (let [ret-1  (assoc arg+
                                              pos-amper
                                              [])
                                arg-1+ (into []
                                             (concat (take pos-amper
                                                           arg+)
                                                     (drop (inc pos-amper)
                                                           arg+)))]
                            ($.test.util/prop+

                              "Direct"
                              (-eval-fn fn-form
                                        arg-1+
                                        ret-1)


                              "Interned"
                              (-eval-fn-def fn-form
                                            arg-1+
                                            ret-1)


                              "Let"
                              (-eval-fn-let fn-form
                                            arg-1+
                                            ret-1)))

                          "Extra argument"
                          (let [ret+1  (update arg+
                                               pos-amper
                                               #(vector 42
                                                        %))
                                arg+1+ (into []
                                             (concat (take pos-amper
                                                           arg+)
                                                     [42]
                                                     (drop pos-amper
                                                           arg+)))]
                            ($.test.util/prop+

                              "Direct"
                              (-eval-fn fn-form
                                        arg+1+
                                        ret+1)

                              "Interned"
                              (-eval-fn-def fn-form
                                            arg+1+
                                            ret+1)

                              "Let"
                              (-eval-fn-let fn-form
                                            arg+1+
                                            ret+1))))))))



(tc.ct/defspec hash--

  ;; Also tests `hash?`.

  (tc.prop/for-all* [($.test.util/generator [:or
                                             :convex/address
                                             :convex/blob-32])]
                    (fn [x]
                      (let [[h
                             h-1?
                             h-2?] ($.test.util/eval ($/templ {'X x}
                                                              '(let [h (hash X)]
                                                                 [h
                                                                  (hash? h)
                                                                  (hash? (hash h))])))]
                         ($.test.util/prop+

                           "Result is a hash"
                           ($.test.util/valid? :convex/hash
                                               h)

                           "Hashing does produce a hash"
                           h-1?

                           "Hashing a hash produces a hash"
                           h-2?)))))



(tc.ct/defspec inc--double

  ;; See [[dec-double]].

  (tc.prop/for-all* [($.test.util/generator [:double
                                             {:min (double Long/MIN_VALUE)}])]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'inc
                                                        x))]
                        ($.test.util/prop+

                          "Result is always a long"
                          (int? x-2)

                          "Overflow"
                          (if (>= x
                                  Long/MAX_VALUE)
                            (= x-2
                               Long/MIN_VALUE)
                            true)

                          "Increment in long range"
                          (if (< Long/MIN_VALUE
                                 x
                                 Long/MAX_VALUE)
                            (= x-2
                               (inc (long x)))
                            true))))))



(tc.ct/defspec inc--long

  (tc.prop/for-all* [($.test.util/generator :convex/long)]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'inc
                                                        x))]
                        ($.test.util/prop+

                          "Result is always a long"
                          (int? x-2)

                          "Increment or overflow"
                          (= x-2
                             (if (= x
                                    Long/MAX_VALUE)
                               Long/MIN_VALUE
                               (inc x))))))))



(tc.ct/defspec keyword?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'keyword?
                        #{:convex/keyword}
                        keyword?))



(tc.ct/defspec keyword?--true

  (prop-pred-data-true 'keyword?
                       :convex/keyword
                       keyword?))



;; TODO. `log`, weird, no docstring and behaves like `vector`



(tc.ct/defspec list?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'list?
                        #{:convex/list}
                        list?))



(tc.ct/defspec list?--true

  {:max-size max-size-coll}

  (prop-pred-data-true 'list?
                       :convex/list
                       list?))



(tc.ct/defspec long?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'long?
                        #{:convex/long}
                        int?))



(tc.ct/defspec long?--true

  (prop-pred-data-true 'long?
                       :convex/long
                       int?))



(tc.ct/defspec map?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'map?
                        #{:convex/map}
                        map?))



(tc.ct/defspec map?--true

  {:max-size max-size-coll}

  (prop-pred-data-true 'map?
                       :convex/map
                       map?))



(tc.ct/defspec max--

  (prop-compare 'max
                max))



(tc.ct/defspec min--

  (prop-compare 'min
                min))



(tc.ct/defspec nil?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'nil?
                        #{:convex/nil}
                        nil?))



(t/deftest nil?--true

  (t/is (true? (nil? ($.test.util/eval nil))))

  (t/is (true? (nil? ($.test.util/eval '(do nil))))))



(tc.ct/defspec number?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'number?
                        #{:convex/boolean ;; TODO. See #73.
                          :convex/char    ;; TODO. See #68.
                          :convex/double
                          :convex/long}
                        number?))



(tc.ct/defspec number?--true

  (prop-pred-data-true 'number?
                       :convex/number
                       number?))



(tc.ct/defspec pow--

  (tc.prop/for-all* [($.test.util/generator [:tuple
                                             :convex/number
                                             :convex/number])]
                    (fn [[x y]]
                      ($.test.util/eq (StrictMath/pow x
                                                      y)
                                      ($.test.util/eval (list 'pow
                                                              x
                                                              y))))))



(tc.ct/defspec set?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'set?
                        #{:convex/set}
                        set?))



(tc.ct/defspec set?--true

  {:max-size max-size-coll}

  (prop-pred-data-true 'set?
                       :convex/set
                       set?))



(tc.ct/defspec signum--

  (tc.prop/for-all* [($.test.util/generator :convex/number)]
                    (fn [x]
                      (let [x-2 ($.test.util/eval (list 'signum
                                                        x))]
                        ($.test.util/prop+

                          "Negative"
                          (if (neg? x)
                            (= -1
                               x-2)
                            true)

                          "Positive"
                          (if (pos? x)
                            (= 1
                               x-2)
                            true)

                          "Zero"
                          (if (zero? x)
                            (zero? x-2)
                            true))))))



(tc.ct/defspec sqrt--

  (prop-clojure 'sqrt
                [:tuple :convex/number]
                #(StrictMath/sqrt %)))



(tc.ct/defspec str?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'str?
                        #{:convex/string}
                        string?))



(tc.ct/defspec str?--true

  (prop-pred-data-true 'str?
                       :convex/string
                       string?))



(tc.ct/defspec vector?--false

  {:max-size max-size-coll}

  (prop-pred-data-false 'vector?
                        #{:convex/vector}
                        vector?))



(tc.ct/defspec vector?--true

  {:max-size max-size-coll}

  (prop-pred-data-true 'vector?
                       :convex/vector
                       vector?))
