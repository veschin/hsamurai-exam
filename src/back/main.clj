(ns back.main
  (:require
   [route-map.core :as rm]
   [ring.util.response :refer [file-response not-found]]
   [ring.adapter.jetty]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   back.api
   [clojure.test :as t]))

(def routes
  {"patients" {[:view] {:GET (constantly (file-response "resources/index.html"))}}
   "api"      back.api/routes})

(defn dispatcher [{:keys [uri request-method] :as req}]
  (prn uri)
  (if-let [match (:match (rm/match [request-method uri] routes))]
    (match req)
    (not-found "Path not found")))

(def server
  (ring.adapter.jetty/run-jetty
   (-> dispatcher
       (wrap-params)
       (wrap-keyword-params)
       (wrap-json-params)
       (wrap-resource "public"))
   {:port  8080
    :join? false}))

(comment

  (.start server)
  (.stop server)

  (:match (rm/match [:get "/patients/list"] routes))
  ;;
  )
