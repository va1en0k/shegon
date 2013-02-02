(ns shegon.repl
  (:require [shegon.user :as u])
  (:use [jayq.core :only [$ ajax]]))


(def input (atom nil))
(def $input (atom nil))

(declare do-repl)

(def input-keymap
  (u/js-map :Enter #(do-repl)))

(defn $repl-el [] ($ "#repl"))
(defn scroll-down [] (.scrollTop ($repl-el) (.-scrollHeight (first ($repl-el)))))

(defn last-pos [cm]
  (let [last-line (- (.lineCount cm) 1)]
    (u/js-map :line last-line
              :ch (.-length (.getLine cm last-line)))))

(defn create-input []
  (reset! input
    (js/CodeMirror.
      (first ($repl-el))
      (u/js-map :value "(+ 1 2)"
                :extraKeys input-keymap)))
  (.focus @input)
  (.setCursor @input (last-pos @input))
  (reset! $input ($ (.getWrapperElement @input)))
  (.addClass @$input "input")
  (.on @input "change" scroll-down))

(defn add-promt []
  (js/CodeMirror.
    #(.addClass (.insertBefore ($ %) @$input) "promt")
      (u/js-map :value "shegon.user=>"
                :readOnly true)))

(defn current-promt []
  (.last ($ ".CodeMirror.promt")))

(defn add-output [output]
  (.on
    (js/CodeMirror.
      #(.addClass (.insertBefore ($ %) (shegon.repl/current-promt)) "output")
        (u/js-map :value (str output)
                  :readOnly true))
    "focus" focus-only-input)
  (scroll-down))

(defn do-repl []
  (let [inp (.getValue @input)]
    (add-output (str "shegon.user=>  " inp))
    (compile-js inp #(add-output (do-eval %)))
    (.setValue @input "")))

(defn do-eval [code]
  (js/eval code))

(defn compile-js [code callback]
  (let [pr (ajax "/compiler" {:type :post
                              :data {:source code}
                              :dataType :jsonp})]
    (.done pr (fn [data] (callback (.-result data))))
    (.fail pr (fn [error] (js/console.log error)))))

(defn focus-only-input [cm]
  (let [sel (.getSelection cm)]
    (when (= sel "")
      (js/setTimeout #(.focus @input) 100))))

($ (fn []

    (.html ($repl-el) "")

    (create-input)
    (add-promt)
    (add-output "(help)")

    ))
