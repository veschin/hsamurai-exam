(ns back.db.connection
  (:require [clojure.java.jdbc :as jdbc]))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   "postgresdb"
   :host     "127.0.0.1"
   :user     "admin"
   :password "adminS3cret"})

(comment

  ()
  (jdbc/db-connection db)

  ;;
  )
