(ns emissary.core
  (:require [emissary.dispatcher :as dispatcher]
            [emissary.dispatcher.async :as async]
            [emissary.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; coeffects                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti coeffect "Load context for input to event handlers"
  (fn [cfx-id _] cfx-id))

(defn coeffects [context]
  (util/initialize-map context coeffect))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; side effects                                                             ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti side-effect "Execute side effects specified by event handlers"
  (fn [sfx-id _] sfx-id))

(defn side-effects [effect-map]
  (util/initialize-map effect-map side-effect))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatcher [event-map]
  (-> {:events event-map, :coeffects coeffects, :side-effects side-effects}
      async/build
      async/init))

(defn dispatch [dispatcher event]
  (dispatcher/dispatch dispatcher event)
  true)

(defn shutdown [dispatcher]
  (async/halt dispatcher))
