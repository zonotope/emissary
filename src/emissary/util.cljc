(ns emissary.util)

(defn initialize-map [m f]
  (reduce-kv (fn [m* k v]
               (assoc m* k (f k v)))
             {} m))
