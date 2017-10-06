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

(defn- event-handler [{:keys [context handler] :as config} cofx-fn sfx-fn]
  (fn [evt]
    (-> context
        cofx-fn
        (assoc ::event evt)
        handler
        sfx-fn)))

(defn- handler->queue [handler]
  (let [q (chan)]
    (go-loop []
      (when-let [evt (<! q)]
        (handler evt)
        (recur)))
    q))

(defn- ->event-handler-queues [events cofx-fn sfx-fn]
  (util/initialize-map events (fn [_ handler-config]
                                (-> handler-config
                                    (event-handler cofx-fn sfx-fn)
                                    handler->queue))))

(defn- route-events! [event-queue handler-queues]
  (let [broker (pub event-queue event/get-id)]
    (doseq [[event-id handler-queue] handler-queues]
      (sub broker event-id handler-queue))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Lifecycle
  (init [dispatcher]
    "Initialize any dispatcher state required to dispatch events.")
  (halt [dispatcher]
    "Tear down any state associated with `dispatcher`."))

(deftype AsyncDispatcher [event-queue handler-queues]
  Lifecycle
  (init [dispatcher]
    (route-events! event-queue handler-queues)
    dispatcher)

  (halt [dispatcher]
    (close! event-queue))

  Dispatcher
  (dispatch [_ event]
    (go (>! event-queue event))
    nil))

(defn build [{:keys [events coeffects side-effects]}]
  (let [event-queue (chan)
        handler-queues (->event-handler-queues events coeffects side-effects)]
    (->AsyncDispatcher event-queue handler-queues)))
