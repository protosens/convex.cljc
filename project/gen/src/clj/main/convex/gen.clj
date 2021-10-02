(ns convex.gen

  "`test.check` generators for cells."

  {:author "Adam Helinski"}

  (:import (convex.core.lang Symbols))
  (:refer-clojure :exclude [boolean
                            byte
                            char
                            double
                            long
                            keyword
                            list
                            map
                            set
                            string
                            symbol
                            vector])
  (:require [clojure.string]
            [clojure.set]
            [convex.cell                   :as $.cell]
            [convex.std                    :as $.std]
            [clojure.test.check.generators :as TC.gen]))


;;;;;;;;;; Private


(def ^:private -byte

  ;; Generates bytes as longs.

  (TC.gen/choose 0
                 255))



(defn- -map

  ;; Helper for [[blob-map]] and [[map]].


  ([f gen-k gen-v]

   (TC.gen/fmap f
                (TC.gen/vector-distinct-by first
                                           (TC.gen/tuple gen-k
                                                         gen-v))))


  ([f gen-k gen-v n]

   (TC.gen/fmap f
                (TC.gen/vector-distinct-by first
                                           (TC.gen/tuple gen-k
                                                         gen-v)
                                           {:num-elements n})))


  ([f gen-k gen-v n-min n-max]

   (TC.gen/fmap f
                (TC.gen/vector-distinct-by first
                                           (TC.gen/tuple gen-k
                                                         gen-v)
                                           {:min-elements n-min
                                            :max-elements n-max}))))



(def ^:private -string-symbolic

  ;; JVM string for building keyword and symbol cells.

  (TC.gen/fmap clojure.string/join
               (TC.gen/vector TC.gen/char-alphanumeric
                              1
                              64)))



(defn- -sequential

  ;; Helper for [[list]] and [[vector]].


  ([f gen]

   (TC.gen/fmap f
                (TC.gen/vector gen)))


  ([f gen n]

   (TC.gen/fmap f
                (TC.gen/vector gen
                               n)))


  ([f gen n-min n-max]

   (TC.gen/fmap f
                (TC.gen/vector gen
                               n-min
                               n-max))))



(defn- -vec->blob

  ;; Converts a Clojure vector of bytes to a blob.

  [v]

  (-> v
      byte-array
      $.cell/blob))



(defn- -vec->string

  ;; Converts a Clojure vector of chars to a string cell.

  [v]

  (-> v
      clojure.string/join
      $.cell/string))


;;;;;;;;;; Miscellaneous


(defn quoted

  "Wraps the given `gen` so that the output is wrapped in a `quote` form."

  [gen]

  (TC.gen/fmap (fn [x]
                 ($.cell/list [Symbols/QUOTE
                               x]))
               gen))


;;;;;;;;;; Scalar cells


(def address

  "Address cell."

  (TC.gen/fmap $.cell/address
               (TC.gen/large-integer* {:min 0})))



(defn blob

  "Blob cell.
  
   When length is not given, depends on current `test.check` size."


  ([]

   (TC.gen/fmap -vec->blob
                (TC.gen/vector -byte)))


  ([n]

   (TC.gen/fmap -vec->blob
                (TC.gen/vector -byte
                               n)))


  ([n-min n-max]

   (TC.gen/fmap -vec->blob
                (TC.gen/vector -byte
                               n-min
                               n-max))))



(def blob-32

  "32-byte blob cell.

   Useful for CVM hashes and keys."

  (blob 32))



(def boolean

  "Boolean cell."

  (TC.gen/fmap $.cell/boolean
               TC.gen/boolean))



(def byte

  "Byte cell."

  (TC.gen/fmap $.cell/byte
               -byte))



(def char

  "Char cell between 0 and 255 inclusive."

  (TC.gen/fmap $.cell/char
               TC.gen/char))



(def char-alphanum

  "Like [[char]] but alphanumeric, hence always printable."

  (TC.gen/fmap $.cell/char
               TC.gen/char-alphanumeric))



(def double

  "Double cell."

  (TC.gen/fmap $.cell/double
               TC.gen/double))



(def keyword

  "Keyword cell."

  (TC.gen/fmap $.cell/keyword
               -string-symbolic))



(def long

  "Long cell."

  (TC.gen/fmap $.cell/long
               TC.gen/large-integer))



(def number

  "Either [[double]] or [[long]]."

  (TC.gen/one-of [double
                  long]))



(def nothing

  "Generates nil."

  (TC.gen/return nil))



(defn string

  "String cell containing [[char]]."


  ([]

   (TC.gen/fmap $.cell/string
                TC.gen/string))


  ([n]

   (TC.gen/fmap -vec->string
                (TC.gen/vector char
                               n)))


  ([n-min n-max]

   (TC.gen/fmap -vec->string
                (TC.gen/vector char
                               n-min
                               n-max))))



(defn string-alphanum

  "String cell containing [[char-alphanum]]."


  ([]

   (TC.gen/fmap $.cell/string
                TC.gen/string-alphanumeric))


  ([n]

   (TC.gen/fmap -vec->string
                (TC.gen/vector char-alphanum
                               n)))


  ([n-min n-max]

   (TC.gen/fmap -vec->string
                (TC.gen/vector char-alphanum
                               n-min
                               n-max))))



(def symbol

  "Symbol cell."

  (TC.gen/fmap $.cell/symbol
               -string-symbolic))


;;;


(def scalar

  "Any CVM cell that is not a collection:

   - [[address]]
   - [[blob]]
   - [[boolean]]
   - [[byte]]
   - [[char-alphanum]]
   - [[double]]
   - [[keyword]]
   - [[long]]
   - [[nothing]]
   - [[string-alphanum]]
   - [[symbol]]
  
  This excludes non-CVM cells such as the different transaction types."

  (TC.gen/one-of [address
                  (blob)
                  boolean
                  byte
                  char-alphanum
                  double
                  keyword
                  long
                  nothing
                  (string-alphanum)
                  symbol]))


;;;;;;;;;; Collection cells


(defn list

  "List cell where item are generated using `gen`.
  
   When length target is not provided, depends on current `test.check` size."


  ([gen]

   (-sequential $.cell/list
                gen))


  ([gen n]
   
   (-sequential $.cell/list
                gen
                n))


  ([gen n-min n-max]

   (-sequential $.cell/list
                gen
                n-min
                n-max)))



(defn blob-map

  "Blob map here item are generated using `gen`.
   
   Generator for keys must output [[blob]] or specialized blob like [[address]].
  
   When length target is not provided, depends on current `test.check` size."


  ([gen-k gen-v]

   (-map $.cell/blob-map
         gen-k
         gen-v))


  ([gen-k gen-v n]

   (-map $.cell/blob-map
         gen-k
         gen-v
         n))


  ([gen-k gen-v n-min n-max]

   (-map $.cell/blob-map
         gen-k
         gen-v
         n-min
         n-max)))



(defn map

  "Map cell where item are generated using `gen`.
  
   When length target is not provided, depends on current `test.check` size."


  ([gen-k gen-v]

   (-map $.cell/map
         gen-k
         gen-v))


  ([gen-k gen-v n]

   (-map $.cell/map
         gen-k
         gen-v
         n))


  ([gen-k gen-v n-min n-max]

   (-map $.cell/map
         gen-k
         gen-v
         n-min
         n-max)))



(defn set

  "Set cell where item are generated using `gen`.
  
   When length target is not provided, depends on current `test.check` size."


  ([gen]

   (TC.gen/fmap $.cell/set
                (TC.gen/vector-distinct gen)))


  ([gen n]

   (TC.gen/fmap $.cell/set
                (TC.gen/vector-distinct gen
                                        {:num-elements n})))


  ([gen n-min n-max]

   (TC.gen/fmap $.cell/set
                (TC.gen/vector-distinct gen
                                        {:min-elements n-min
                                         :max-elements n-max}))))



(defn vector

  "Vector cell where item are generated using `gen`.
  
   When length target is not provided, depends on current `test.check` size."


  ([gen]

   (-sequential $.cell/vector
                gen))


  ([gen n]

   (-sequential $.cell/vector
                gen
                n))


  ([gen n-min n-max]

   (-sequential $.cell/vector
                gen
                n-min
                n-max)))


;;;;;;;;;; Recursive cells


(def recursive

  "Base generators for recursive collection cells where an item of a collection can be a collection as well.
  
   Leaves are [[scalar]] while containers can be:
  
   - [[blob-map]]
   - [[list]]
   - [[map]]
   - [[set]]
   - [[vector]]
  
   Produces a [[scalar]] in roughly 10% of outputs."

  (TC.gen/recursive-gen (fn [gen-inner]
                          (let [scale-map (fn [size]
                                            (quot size
                                                  2))]
                            (TC.gen/one-of [(list gen-inner)
                                            (TC.gen/scale scale-map
                                                          (map gen-inner
                                                               gen-inner))
                                            (TC.gen/scale scale-map
                                                          (blob-map (TC.gen/one-of [address
                                                                                    (blob)])
                                                                    gen-inner))
                                            (set gen-inner)
                                            (vector gen-inner)])))
                        scalar))



(def any

  "Combines [[scalar]] and [[recursive]] to produce any CVM cell."

  (TC.gen/frequency [[55 recursive]
                     [45 scalar]]))



(def any-list

  "Recursive list cell where an item can be any cell."

  (TC.gen/fmap (fn [x]
                 (cond
                   ($.std/list? x)   x
                   ($.std/map? x)    (reduce (fn [acc [k v]]
                                               (-> acc
                                                   ($.std/conj k)
                                                   ($.std/conj v)))
                                             ($.cell/list)
                                             x)
                   ($.std/set? x)    ($.std/into ($.cell/list)
                                                 x)
                   ($.std/vector? x) ($.cell/list x)
                   :else             ($.std/list x)))
               recursive))



(let [-to-map (fn [coll]
                ($.std/into ($.cell/map)
                            (clojure.core/map vec)
                            (partition 2
                                       2
                                       coll
                                       coll)))]
  (def any-map

    "Recursive hash map cell where an item can be any cell."
    
    (TC.gen/fmap (fn [x]
                   (cond
                     (map? x)    x
                     (list? x)   (-to-map x)
                     (set? x)    (-to-map x)
                     (vector? x) (-to-map x)
                     :else       ($.std/hash-map x
                                                 x)))
                 recursive)))



(def any-set

  "Recursive set cell where an item can be any cell."

  (TC.gen/fmap (fn [x]
                 (cond
                   ($.std/list? x)   ($.std/set x)
                   ($.std/map? x)    (reduce (fn [acc [k v]]
                                               (-> acc
                                                   ($.std/conj k)
                                                   ($.std/conj v)))
                                             ($.cell/set)
                                             x)
                   ($.std/set? x)    x
                   ($.std/vector? x) ($.std/set x)
                   :else             ($.std/hash-set x)))
               recursive))



(def any-vector

  "Recursive vector cell where an item can be any cell."

  (TC.gen/fmap (fn [x]
                 (cond
                   ($.std/list? x)   ($.std/vec x)
                   ($.std/map? x)    (reduce (fn [acc [k v]]
                                               (-> acc
                                                   ($.std/conj k)
                                                   ($.std/conj v)))
                                             ($.cell/vector)
                                             x)
                   ($.std/set? x)    ($.std/vec x)
                   ($.std/vector? x) x
                   :else             ($.std/vector x)))
               recursive))
