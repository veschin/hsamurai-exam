(ns back.main
  (:gen-class)
  (:require
   [route-map.core :as rm]
   [ring.util.response :refer [file-response not-found]]
   [ring.adapter.jetty]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   back.api
   [clojure.test :as t]
   
   [back.db.dsl :as dsl]
   [back.db.schema :refer [schema]]
   ))

(def routes
  {"patients" {[:view] {:GET (constantly (file-response "resources/index.html"))
                        [:id] {:GET (constantly (file-response "resources/index.html"))}}}
   "api"      back.api/routes})

(defn dispatcher [{:keys [uri request-method] :as req}]
  (prn uri)
  (if-let [match (:match (rm/match [request-method uri] routes))]
    (match req)
    (not-found "Path not found")))

(defn server []
  (def jetty-server
    (ring.adapter.jetty/run-jetty
     (-> dispatcher
         (wrap-params)
         (wrap-keyword-params)
         (wrap-json-params)
         (wrap-resource "public"))
     {:port  8080
      :join? false})))

(defn create-tables []
  (let [{patient-fields :patients} schema]
      ;; (dsl/exec! {:drop-table-if-exists :patients})
      (dsl/exec! {:create-table-if-not-exists
                  [:!!patients patient-fields]})))

;; (defn fill-by-random []
  ;; {:})

(defn -main [& _]
  (create-tables)
  (server))

(comment
  
  (.start jetty-server)
  (.stop jetty-server)
  (dsl/exec! {:drop-table-if-exists :patients})
  
  (:match (rm/match [:get "/patients/list"] routes))
  ;;
  )
