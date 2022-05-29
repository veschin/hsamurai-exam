(ns back.api
  (:require [back.db.dsl :as dsl]
            [ring.util.response :refer [response not-found]]
            cheshire.core
            [clojure.test :as t]
            back.db.schema
            ;; front.spec.spec-create
            ))

(defn create [{params :params}]
  (try
    (do (dsl/insert! :patients params)
        (response "201:Created successfully"))
    (catch Exception e {:status 500 :body e})))

(defn edit [req])
(defn delete [req])
(defn list- [_ & [query-hash-map]]
  (if-let [resp (dsl/query
                 (or query-hash-map
                     {:select :*
                      :from   :patients
                      :limit  15}))]
    (response (cheshire.core/generate-string resp))
    (not-found "Empty list")))

(defn search [{params :params}]
  (list-
   nil
   {:select :*
    :from   :patients
    :where
    (->> back.db.schema/schema
         :patients
         (#(dissoc % :id))
         keys
         (map #(conj [:regex % (get params "search-value")]))
         (into [:or]))}))

(t/deftest valid-search?
  (letfn
   [(query-by-regex [value]
      (-> {:params {"search-value" value}}
          search
          :status
          #{200}
          boolean))]
    (t/is (query-by-regex "19"))
    (t/is (query-by-regex "Оле"))
    (t/is (not (query-by-regex "!#")))
    (t/is (query-by-regex ","))))

(def routes
  {"create" {:POST create}
   "edit"   {:PACTH edit}
   "delete" {:DELETE delete}
   "list"   {:GET list-}
   "search" {:GET search}})

(comment
  
  (let [->with-ns (fn [[key val]] {(keyword "data" (name key)) val})]
    (->> req-
         (map ->with-ns)
         (reduce into {})
         ;; (front.spec.spec-create/valid?)
         ))
  (valid-search?)
  (dsl/query {:select :* :from :patients})
  (keyword "ns" "name")
;;
  )
