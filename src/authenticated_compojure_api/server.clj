(ns authenticated-compojure-api.server
  (:require [org.httpkit.server :as httpkit]
            [authenticated-compojure-api.queries.query-defs :as query]
            [authenticated-compojure-api.handler :refer [app]]))

(defn create-tables []
  (query/create-registered-user-table-if-not-exists!)
  (query/create-permission-table-if-not-exists!)
  (query/create-user-permission-table-if-not-exists!)
  (query/create-password-reset-key-table-if-not-exists!))

(defn -main [port]
  (create-tables)
  (httpkit/run-server app {:port (Integer/parseInt port) :join false})
  (println "server started on port:" port))
