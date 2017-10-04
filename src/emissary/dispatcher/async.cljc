(ns emissary.dispatcher.async
  (:require [emissary.dispatcher :as dispatcher :refer [Dispatcher]]
            [emissary.event :as event]
            [emissary.util :as util]
            [clojure.core.async :as core.async
                                :refer [<! >! chan close! go go-loop pipe pub
                                        sub]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event handling                                                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- event-handler [{:keys [context handler] :as config} cofx-fn]
  (fn [evt]
    (-> context
        cofx-fn
        (assoc ::event evt)
        handler)))

(defn- handler->queue [handler]
  (chan 1 (map handler)))

(defn- ->event-handler-queues [events cofx-fn]
  (util/initialize-map events (fn [_ handler-config]
                                (-> handler-config
                                    (event-handler cofx-fn)
                                    handler->queue))))

(defn- route-events! [event-queue handler-queues]
  (let [broker (pub event-queue event/get-id)]
    (doseq [[event-id handler-queue] handler-queues]
      (sub broker event-id handler-queue))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; side effects                                                             ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ->side-effect-queue [sfx-fn]
  (let [q (chan)]
    (go-loop []
      (when-let [sfx (<! q)]
        (sfx-fn sfx)
        (recur)))
    q))

(defn- route-side-effects! [handler-queues side-effect-queue]
  (-> handler-queues
      vals
      core.async/merge
      (pipe side-effect-queue)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Lifecycle
  (init [dispatcher]
    "Initialize any dispatcher state required to dispatch events.")
  (halt [dispatcher]
    "Tear down any state associated with `dispatcher`."))

(deftype AsyncDispatcher [event-queue handler-queues side-effect-queue]
  Lifecycle
  (init [dispatcher]
    (route-events! event-queue handler-queues)
    (route-side-effects! handler-queues side-effect-queue)
    dispatcher)

  (halt [dispatcher]
    (close! event-queue))

  Dispatcher
  (dispatch [_ event]
    (go (>! event-queue event))))

(defn build [{:keys [events coeffects side-effects]}]
  (let [event-queue (chan)
        handler-queues (->event-handler-queues events coeffects)
        side-effect-queue (->side-effect-queue side-effects)]
    (->AsyncDispatcher event-queue handler-queues side-effect-queue)))
