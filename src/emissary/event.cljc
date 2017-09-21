(ns emissary.event)

(defprotocol Event
  (get-id [event]))

(extend-protocol Event
  clojure.lang.Keyword
  (get-id [event]
    event)

  clojure.lang.MapEntry
  (get-id [event-ety]
    (key event-ety))

  clojure.lang.PersistentVector
  (get-id [event-vec]
    (first event-vec)))
