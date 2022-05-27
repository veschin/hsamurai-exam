(ns back.api)

(defn create [req] (def req- req))
(defn edit [req])
(defn delete [req])

(def routes
  {"create" {:POST create}
   "edit"   {:PACTH edit}
   "delete" {:DELETE delete}})
