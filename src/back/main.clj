(ns back.main
  (:gen-class)
  (:require
   [ring.util.response :refer [file-response not-found]]
   [ring.adapter.jetty]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.json :refer [wrap-json-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]

   [route-map.core :as rm]
   [clojure.test :as t]

   [back.api]
   [back.db.dsl :as dsl]
   [back.db.schema :refer [schema]]))

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

(defn create-and-fill-tables []
  (let [{patient-fields :patients} schema]
    (dsl/exec! {:drop-table-if-exists :patients})
    (dsl/exec! {:create-table-if-not-exists
                [:!!patients patient-fields]})
    (try
      (do
        (require 'clojure.tools.reader)
        (doseq [patient (clojure.tools.reader/read-string (slurp "patients.edn"))]
          (dsl/insert! :patients patient)))
      (catch Exception e (str "Edn file corrupted or doesn`t exist\n error->" e)))))

(defn -main [& _]
  (create-and-fill-tables)
  (server))

(t/deftest valid-patients-routes?
    (let [request-map (fn [uri] {:request-method :get :uri uri})
          test-get #(-> % request-map dispatcher :status #{200} boolean)]
      (t/is (test-get "/patients/list"))
      (t/is (test-get "/patients/create"))
      (t/is (test-get "/patients/edit"))
      (t/is (test-get "/patients/any-other-page"))
      (t/is (not (test-get "/patients")))
      (t/is (not (test-get "/")))))

(comment
  (.start jetty-server)
  (.stop jetty-server)
  
  (valid-patients-routes?)
  ;;
  )
