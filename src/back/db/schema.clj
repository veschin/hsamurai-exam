(ns back.db.schema
  )

(def schema
  {:patients {:id      [:serial :primary :key]
              :name    :text
              :mname   :text
              :lname   :text
              :sex     :text
              :bdate   :text
              :address :text
              :oms_num :text}})

(def schema-docs
  {:patients {:sex     "Male or Female value"
              :bdate   "dd.mm.yyyy value"
              :address "Unlimited field, separated by comma"
              :oms_num "16`n length field"}})

(comment
  (require '[back.db.dsl :as dsl]
           '[back.db.schema :refer [schema]])
  (dsl/exec! {:drop-table-if-exists :patients})
  
  (let [{patient-fields :patients} schema]
    (dsl/exec! {:create-table-if-not-exists
                [:!!patients patient-fields]}))

  (let [{patient-fields :patients} schema]
    (dsl/parse-sql {:create-table-if-not-exists
                [:!!patients patient-fields]}))
  
  (dsl/table-info :patients)

  (let [oms (reduce str (map (fn [_] (rand-int 9)) (range 16)))]
    (dsl/insert!
     :patients
     {:name    "John"
      :mname   "Seven"
      :lname   "Doe"
      :sex     "Male"
      :bdate   "07.07.1997"
      :address "Example-city, Example, 21, 120"
      :oms_num oms}))

  (dsl/query {:select :*
              :from   :patients})
;
  )
