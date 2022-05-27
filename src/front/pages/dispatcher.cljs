(ns front.pages.dispatcher
  (:require [route-map.core :as mc]
            front.pages.list
            front.pages.create))

(def routes
  {"patients"
   {"list" {:. front.pages.list/view}
    "create" {:. front.pages.create/view}
    }})

(defn match-route [path]
  (or (:match (mc/match path routes))
      [:div "Impossible but, page not found!"]))

(comment
  (match-route "/")
  (:match (mc/match "/patients/list" routes))
  ;;
  )
