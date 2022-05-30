(ns front.pages.dispatcher
  (:require [route-map.core :as mc]
            front.pages.list
            front.pages.create
            front.pages.edit))

(def routes
  {"patients"
   {"list"   {:. front.pages.list/view}
    "create" {:. front.pages.create/view}
    "edit"   {[:id] front.pages.edit/view}}})

(defn match-route [path]
  (let [{:keys [match params]} (mc/match path routes)]
    (or (if (not-empty params) (match params) match)
        [:div "Impossible but, page not found!"])))

(comment
  (match-route "/")
  (:match (mc/match "/patients/list" routes))
  (not-empty {} )
  (:params (mc/match "/patients/edit/1" routes))
  ;;
  )
