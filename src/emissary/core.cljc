(ns emissary.core
  (:require [emissary.dispatcher :as dispatcher]
            [emissary.dispatcher.async :as async]
            [emissary.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; effects                                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti effect "Load context/execute side effects for event handlers"
  (fn [fx-id _] fx-id))

(defn execute-effects [effect-map]
  (util/initialize-map effect-map effect))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatcher [handler-map]
  (async/build handler-map execute-effects))

(defn dispatch [dispatcher event]
  (dispatcher/dispatch dispatcher event)
  true)
