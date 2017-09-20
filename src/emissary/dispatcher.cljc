(ns emissary.dispatcher)

(defprotocol Dispatcher
  (dispatch [dispatcher event]
    "Execute the event handlers listening for `event`."))
