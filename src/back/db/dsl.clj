(ns back.db.dsl
  (:require [clojure.string :as string]
            [clojure.java.jdbc :as jdbc]
            back.db.connection))

(defn- sql-operator [[fn- & other]]
  (cond
    (#{"regex"} fn-)
    (string/join " ~ " other)

    (re-matches #"!!.*" fn-)
    (-> fn-
        (string/replace "!!" "")
        string/upper-case
        (str  "(" (string/join ", " other) ")"))
    :else
    (string/join (str " " fn- " ") other)))

(defn- join-form [form]
  (string/join
   ", "
   (for [[key val] form
         :let [->val #(cond
                        (number? %) %
                        (string? %)
                        (str "'" % "'")

                        :else
                        (name %))
               val (if (vector? val)
                     (string/join " " (map ->val val))
                     (->val val))]]
     (str (name key) " " val))))

(defn- recursive-cond [form]
  (cond
    (string? form)
    (str "'" form "'")

    (number? form)
    form

    (symbol? form)
    (str "'" form "'")

    (and (keyword? form) (namespace form))
    (str (namespace form) " " (name form))

    (and (keyword? form) (re-matches #".*->.*" (name form)))
    (string/join " as " (string/split (name form) #"->"))

    (keyword? form)
    (name form)

    (or (list? form) (set? form))
    (string/join ", " (map recursive-cond form))

    (map? form)
    (join-form form)

    (vector? form)
    (cond
      (or (-> form meta :exp?)
          (and (-> form first vector? not)
               (->> form first name (re-matches #"!!.*"))))
      (sql-operator (map recursive-cond form))
      :else
      (str "(" (sql-operator (map recursive-cond form)) ")"))))

(defn- ->command [sql-fn value fn?-]
  (declare parse-sql-)
  (str
   (string/upper-case sql-fn)
   (if fn?- "(" " ")
   (cond
     (and
      fn?-
      ((some-fn vector? list?) value))
     (->> value
          (map name)
          (string/join ", "))

     ((some-fn vector? set? list?) value)
     (if
      (-> value first #{:subquery})
       (str " (" (parse-sql- (last value)) ")")
       (recursive-cond value))

     (keyword? value)
     (name value)

     :single value)
   (when fn?- ")")))

(defn- parse-sql- [sql-dsl]
  (string/join
   " "
   (for [[sql-fn value] (if (map? sql-dsl)
                          (vec sql-dsl)
                          (partition-all 2 sql-dsl))
         :let [sql-fn (name sql-fn)
               fn?-   (re-matches #"!!.*" sql-fn)

               sql-fn
               (cond
                 fn?- (apply str (rest sql-fn))
                 :regular (string/join " " (string/split sql-fn #"-")))]]
     (->command sql-fn value fn?-))))

(defn parse-sql
  "{sql-dsl}                    -> {key structure-or-val}
   :key                         -> like :create-if-not-exist
                                   to CREATE IF NOT EXIST
                                   cant be empty

   structure-or-val:
   vec [fn & other]             -> like [:= a b ..] to (a = b) 
                                        [:!!fn a a2 a3 ..] to FN(a, a2, a3)
                                   can be nested like [[] [[]] []]
                                   [:!! a a2 a3] to (a, a2, a3)
   map {:key val}               -> like {field type} to TABLE(field type, ..);
   set - for unordered          -> like #{1 2 3 4} to 3, 2, 1, 4 detect by #'set?
   list - for ordered           -> like (1 2 3 4) to 1, 2, 3, 4 detect by #'list?
   keyword, string, symbol      -> to 'val'
   num                          -> to num

   vec-syntax:
   :!!fn                        -> like [:!!min a b c] to MIN(a,b,c)
                                   can be empty like :!! to \" \"
   :-> :->> :#> :#>>            -> reserved for jsonb support
   "
  [sql-dsl] (str (parse-sql- sql-dsl) ";"))

(defn query
  "default                           -> [{sql-row} {sql-row} ..]
   if result contain only single row -> {sql-row}
   if result empty                   -> false"
  [sql-dsl]
  (let [result
        (jdbc/query
         back.db.connection/db-spec
         (parse-sql sql-dsl))]
    (cond
      (= (count result) 1)
      (first result)

      (empty? result)
      false

      :default
      (vec result))))

(defn insert!
  "-> nil"
  [table sql-dsl]
  (jdbc/insert!
   back.db.connection/db-spec
   table
   sql-dsl)
  nil)

(defn exec!
  "-> nil"
  [sql-dsl]
  (jdbc/execute!
   back.db.connection/db-spec
   (parse-sql sql-dsl))
  nil)

(defn- ->rinsert-map [table key-val return-key]
  {:insert-into [(->> table name (str "!!") keyword)
                 (->> key-val keys (apply list))]
   :values      [:!! (->> key-val vals (apply list))]
   :returning   (or return-key :id)})

(defn rinsert!
  "-> id or return-key"
  ([{:keys [table key-val return-key]}]
   (parse-sql (->rinsert-map table key-val return-key)))
  ([table key-val & [return-key]]
   (query (->rinsert-map table key-val return-key))))

(defn update!
  "-> nil"
  [table cond- key-val]
  (exec!
   {:update table
    :set    (set (map (fn [[key val]] (with-meta [:= key val] {:exp? true})) key-val))
    :where  cond-}))

(defn remove!
  "remove-cond - vec like [fn arg ..]
                 arg can be [fn ..]
   -> nil"
  [table remove-cond]
  (jdbc/execute!
   back.db.connection/db-spec
   (parse-sql
    {:delete-from table
     :where       remove-cond}))
  nil)

(defn table-info
  "-> {:field [:postgres_type clojure_type] ..}"
  [table]
  (let [types {:integer java.lang.Long
               :jsonb   clojure.lang.PersistentArrayMap
               :text    java.lang.String}]
    (->> {:select #{:column_name :data_type}
          :from    :information_schema.columns
          :where   [:= :table_name (-> table name symbol)]}
         query
         (map
          (comp (partial map keyword)
                reverse
                vals))
         (map (fn [[key val]] {key [val (val types)]}))
         (into {}))))
