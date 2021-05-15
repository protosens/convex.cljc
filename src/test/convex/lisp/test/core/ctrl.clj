(ns convex.lisp.test.core.ctrl

  "Testing flow control constructs."

  {:author "Adam Helinski"}

  (:require [clojure.string]
            [convex.lisp.form      :as $.form]
            [convex.lisp.test.eval :as $.test.eval]
            [convex.lisp.test.prop :as $.test.prop]))


;;;;;;;;;; 


($.test.prop/deftest ^:recur and-or

  ($.test.prop/check [:vector
                      :convex/data]
                     (fn [x]
                       (let [x-quoted  (map $.form/quoted
                                            x)
                             assertion (fn [sym]
                                         ($.test.eval/like-clojure? (list* sym
                                                                           x-quoted)))]
                         ($.test.prop/mult*

                           "`and` consistent with Clojure"
                           (assertion 'and)

                           "`or` consistent with Clojure"
                           (assertion 'or))))))



($.test.prop/deftest ^:recur if-like

  ($.test.prop/check [:tuple
                      [:and
                       :convex/symbol
                       [:fn #(not (clojure.string/includes? (str %)
                                                            "."))]]
                      :convex/data]
                     (fn [[sym x]]
                       ($.test.prop/mult*

                         "`if` is consistent with Clojure"
                         ($.test.eval/like-clojure? ($.form/templ {'?x x}
                                                                  '(if '?x
                                                                     :true
                                                                     :false)))

                         "`if-let` is consistent with Clojure"
                         ($.test.eval/like-clojure? ($.form/templ {'?sym sym
                                                                   '?x   x}
                                                                  '(if-let [?sym '?x]
                                                                     :true
                                                                     :false)))

                         "`when` is consistent with Clojure"
                         ($.test.eval/like-clojure? ($.form/templ {'?x x}
                                                                  '(when '?x
                                                                     :true)))

                         "`when-let` is consistent with Clojure"
                         ($.test.eval/like-clojure? ($.form/templ {'?sym sym
                                                                   '?x   x}
                                                                  '(when-let [?sym '?x]
                                                                     :true)))))))


;;;;;;;;;;


; assert
; cond
; fail
; halt
; return
; rollback
