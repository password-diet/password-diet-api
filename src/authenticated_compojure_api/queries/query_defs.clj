(ns authenticated-compojure-api.queries.query-defs
  (:require [environ.core :refer [env]]
            [yesql.core   :refer [defqueries]]))

(def db-connection {:connection (env :database-url)})

(defqueries "authenticated_compojure_api/tables/user/registered_user.sql"     db-connection)
(defqueries "authenticated_compojure_api/tables/user/permission.sql"          db-connection)
(defqueries "authenticated_compojure_api/tables/user/user_permission.sql"     db-connection)
(defqueries "authenticated_compojure_api/tables/user/password_reset_key.sql"  db-connection)
(defqueries "authenticated_compojure_api/queries/user/registered_user.sql"    db-connection)
(defqueries "authenticated_compojure_api/queries/user/permission.sql"         db-connection)
(defqueries "authenticated_compojure_api/queries/user/user_permission.sql"    db-connection)
(defqueries "authenticated_compojure_api/queries/user/password_reset_key.sql" db-connection)
