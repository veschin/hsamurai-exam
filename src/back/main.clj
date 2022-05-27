(ns back.main
  (:require
   [route-map.core :as rm]
   [ring.util.response :refer [file-response not-found]]
   [ring.adapter.jetty]
   [ring.middleware.resource :refer [wrap-resource]]
   back.api
   [clojure.test :as t]))

(def routes
  {"patients" {[:view] {:GET (constantly (file-response "resources/index.html"))}}
   "api" back.api/routes})

(defn dispatcher [{:keys [uri request-method] :as req}]
  (prn uri)
  (if-let [match (:match (rm/match [request-method uri] routes))]
    (match req)
    (not-found "Path not found")))

(def server
  (ring.adapter.jetty/run-jetty
   (wrap-resource dispatcher "public")
   {:port  8080
    :join? false}))

(comment

  (.start server)
  (.stop server)

  (Require 'ring.middleware.resource)
  ring.middleware.resource/wrap-resource

  (slurp (:body))
  (slurp "http://localhost:8080/api/create")

  (:match (rm/match [:get "/patients/list"] routes))
  ;;
  )
