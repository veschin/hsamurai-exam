(ns back.main
  (:require
   [route-map.core :as rm]
   [ring.util.response :refer [response not-found]]
   [ring.adapter.jetty]
   [ring.middleware.multipart-params
    :refer [wrap-multipart-params]]

   [clojure.test :as t]))

(def routes
  {:GET (constantly (ring.util.response/file-response "resources/index.html"))})

(defn dispatcher [{:keys [uri request-method] :as req}]
  (prn uri)
  (if-let [match (:match (rm/match [request-method uri] routes))]
    (match req)
    (not-found "Path not found")))

(t/deftest valid-dispatch?
  (t/is
   (= (dispatcher
       {:uri            "/"
        :request-method :get})
      {:status  200
       :headers {}
       :body    123})))

(def server
  (ring.adapter.jetty/run-jetty
   dispatcher
   {:port  8080
    :join? false}))

(comment
  (valid-dispatch?)

  (.start server)
  (.stop server)
  (require)
  ring.middleware.resource/wrap-resource
  (slurp (:body ))
  (slurp "http://localhost:8080")

  ;;
  )
