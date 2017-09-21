(ns emissary.dispatcher.async
  (:require [emissary.dispatcher :as dispatcher :refer [Dispatcher]]
            [emissary.event :as event]
            [emissary.util :as util]
            [clojure.core.async :refer [<! >! chan go go-loop pipe pub sub]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; event handler queues                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- event->context [event]
  {::dispatcher/event event})

(defn- merge-coeffect-context-fn [cofx-fn cofx-ctx]
  (fn [ctx]
    (cond-> ctx
      cofx-ctx (merge (cofx-fn cofx-ctx)))))

(defn- event-handler-transducer [handler merge-cofx]
  (map (comp handler merge-cofx event->context)))

(defn- event-handler->queue [{:keys [context handler]} coeffects]
  (let [merge-cofx (merge-coeffect-context-fn coeffects context)
        event-handler-xf (event-handler-transducer handler merge-cofx)]
    (chan 1 event-handler-xf)))

(defn- map->event-handler-queues [handler-map coeffect-fn]
  (util/initialize-map handler-map
                       (fn [_ handler-config]
                         (event-handler->queue handler-config coeffect-fn))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; side effect queue                                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- side-effect-queue [side-effect-fn]
  (let [queue (chan)]
    (go-loop []
      (let [pending (<! queue)]
        (side-effect-fn pending)
        (recur)))
    queue))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dispatcher                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype AsyncDispatcher [event-queue broker handler-queues effect-queue]
  Dispatcher
  (dispatch [_ event]
    (go (>! event-queue event))))

(defn- wire-async-queues! [broker handler-queues effect-queue]
  (doseq [[event-id handler-queue] handler-queues]
    (sub broker event-id handler-queue)
    (pipe handler-queue effect-queue)))

(defn build [handler-map effects-fn]
  (let [event-queue (chan)
        broker (pub event-queue event/get-id)
        handler-queues (map->event-handler-queues handler-map effects-fn)
        effect-queue (side-effect-queue effects-fn)]
    (wire-async-queues! broker handler-queues effect-queue)
    (->AsyncDispatcher event-queue broker handler-queues effect-queue)))
