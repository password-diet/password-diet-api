(ns authenticated-compojure-api.route-functions.auth.get-auth-credentials
  (:require [authenticated-compojure-api.general-functions.user.create-token :refer [create-token]]
            [authenticated-compojure-api.queries.query-defs :as query]
            [ring.util.http-response :as respond]))

(defn auth-credentials-response [request]
  (let [user          (:identity request)
        refresh-token (str (java.util.UUID/randomUUID))
        _ (query/update-registered-user-refresh-token<! {:refresh_token refresh-token :id (:id user)})]
    (respond/ok {:id            (:id user)
                 :username      (:username user)
                 :permissions   (:permissions user)
                 :token         (create-token user)
                 :refreshToken  refresh-token})))
