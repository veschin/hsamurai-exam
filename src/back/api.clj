(ns back.api
  (:require [back.db.dsl :as dsl]
            [ring.util.response :refer [response not-found]]
            cheshire.core
            [clojure.test :as t]
            back.db.schema
            ;; front.spec.spec-create
            ))

(defn query-or-not-found [query-hash-map message404]
  (if-let [resp (dsl/query query-hash-map)]
    (response (cheshire.core/generate-string resp))
    (not-found message404)))

(def insert- #(dsl/insert! :patients %))

(defn create
  "params {key val ..}
   must correspond schema described in back.db.schema/schema
   used in jdbc/insert as values-map argument"
  [{params :params}]
  (try
    (do (insert- params)
        (response "201:Created successfully"))
    (catch Exception e {:status 500 :body e})))

(def edit- #(dsl/update! :patients %1 %2))

(defn edit
  "params {:id id}
   will converted to condition [:= id id]
   in sql update query"
  [{{id :id :as params} :params}]
  (try
    (do (edit- [:= :id id] (dissoc params :id))
        (response "200:Edited successfully"))
    (catch Exception e {:status 500 :body e})))

(defn edit-info
  "params {\"id\" id}
   will converted to condition [:= id id]
   in sql select query"
  [{params :params}]
  (query-or-not-found
   {:select :*
    :from   :patients
    :where  [:= :id (get params "id")]}
   "Patient not found"))

(def remove- #(dsl/remove! :patients %))

(defn delete
  "params {key val ..}
   will convert to condition [:and [:= key val] ..]
   in sql remove query"
  [{params :params}]
  (try
    (do (remove- (->> params
                      vec
                      (map (fn [[key val]] [:= key val]))
                      (reduce conj [:and])))
        (response "200:Deleted successfully"))
    (catch Exception e {:status 500 :body e})))

(defn list-
  "simple get list of patients
   limited by 15n"
  [_ & [query-hash-map]]
  (query-or-not-found
   (or query-hash-map
       {:select :*
        :from   :patients
        :limit  15})
   "Empty list"))

(defn regex-query [regex-val]
  {:select :*
   :from   :patients
   :where
   (->> back.db.schema/schema
        :patients
        (#(dissoc % :id))
        keys
        (map #(conj [:regex % regex-val]))
        (into [:or]))})

(defn search [{params :params}]
  (list- nil (regex-query (get params "search-value"))))

(def routes
  {"create" {:POST   create}
   "edit"   {:GET    edit-info
             :POST   edit}
   "delete" {:POST   delete}
   "list"   {:GET    list-}
   "search" {:GET    search}})

;; ------------TESTING SECTION
(defn- test-response [fn- & key-vals]
  (let [args {:params (->> (partition-all 2 key-vals)
                           (map vec)
                           (into {}))}
        result (fn- args)]
    (t/is result)
    (t/is (-> result
              :status
              #{200}
              boolean))))

(defn- debug-map [& [to-merge value]]
  (merge
   (-> back.db.schema/schema
       :patients
       keys
       (zipmap (repeat (or value "test"))))
   to-merge))

(defn- insert->test->remove [test-data check-fn remove-cond]
  (insert- test-data)
  (t/is check-fn)
  (dsl/remove! :patients remove-cond))

(defn- select-all-by [cond]
  (dsl/query {:select :*
              :from   :patients
              :where  cond}))

(t/deftest valid-create?
  (let [id           999
        test-patient (debug-map {:id id})
        cond- [:= :id id]]
    (insert->test->remove
     test-patient
     #(= test-patient
         (select-all-by cond-))
     cond-)))

(t/deftest valid-search?
  (let [id           999
        test-patient (debug-map {:id id :name "find-me"})]
    (insert->test->remove
     test-patient
     (fn []
       (and
        (= test-patient (dsl/query (regex-query "me")))
        (= test-patient (dsl/query (regex-query "find")))))
     [:= :id id])))

(t/deftest valid-delete?
  (let [id 999
        test-patient (debug-map {:id id})
        cond-         [:= :id id]]
    (insert- test-patient)
    (remove- cond-)
    (t/is (not (select-all-by cond-)))))

(t/deftest valid-edit?
  (let [id            999
        test-patient  (debug-map {:id id})
        edited-fields {:name  "edited"
                       :lname "edited"
                       :sex   "Female"}
        cond-         [:= :id id]]
    (insert->test->remove
     test-patient
     (fn []
       (edit- cond- edited-fields)
       (t/is
        (= edited-fields
           (dsl/query {:select (keys edited-fields)
                       :from   :patients
                       :where  cond-}))))
     cond-)))

(comment

  (dsl/remove! :patients [:= :id 999])

  (edit-info {:params {"id" 1}})
  
  (do
    (valid-create?)
    (valid-delete?)
    (valid-search?)
    (valid-edit?)
    )
  ;;
  )
