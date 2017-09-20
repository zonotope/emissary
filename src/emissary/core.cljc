(ns emissary.core
  (:require [emissary.dispatcher :as dispatcher]
            [emissary.dispatcher.async-dispatcher :as async-dispatcher]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; effects                                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti effect "Load context/execute side effects for event handlers"
  (fn [fx-id _] fx-id))

(defn execute-effects [effect-map]
  (doseq [[fx-id fx-data] effect-map]
    (effect fx-id fx-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatcher [handler-map]
  (async-dispatcher/build handler-map execute-effects))

(defn dispatch [dispatcher event]
  (dispatcher/dispatch dispatcher event)
  true)
