(ns emissary.dispatcher.async-dispatcher
  (:require [emissary.dispatcher :refer [Dispatcher]]
            [emissary.event :as event]
            [clojure.core.async :refer [<! >! chan go go-loop pipe pub sub]]))

(deftype AsyncDispatcher [event-queue broker handler-queues effect-queue]
  Dispatcher
  (dispatch [_ event]
    (go (>! event-queue event))))

(defn- effects->side-effect-queue [effects-fn]
  (let [queue (chan)]
    (go-loop []
      (let [pending (<! queue)]
        (effects-fn pending)
        (recur)))
    queue))

(defn- event-handler->queue [handler]
  (let [handler-xf (map handler)]
    (chan 1 handler-xf)))

(defn- map->event-handler-queues [handler-map]
  (reduce-kv (fn [m event-id handler]
               (assoc m event-id (event-handler->queue handler)))
             {} handler-map))

(defn- wire-async-queues! [broker handler-queues effect-queue]
  (doseq [[event-id handler-queue] handler-queues]
    (sub broker event-id handler-queue)
    (pipe handler-queue effect-queue)))

(defn build [handler-map effects-fn]
  (let [event-queue (chan)
        broker (pub event-queue event/get-id)
        handler-queues (map->event-handler-queues handler-map)
        effect-queue (effects->side-effect-queue effects-fn)]
    (wire-async-queues! broker handler-queues effect-queue)
    (->AsyncDispatcher event-queue broker handler-queues effect-queue)))
