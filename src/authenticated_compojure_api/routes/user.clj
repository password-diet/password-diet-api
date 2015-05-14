(ns authenticated-compojure-api.routes.user
  (:require [authenticated-compojure-api.auth-resources.token-auth-backend :refer [token-backend]]
            [authenticated-compojure-api.middleware.cors :refer [cors-mw]]
            [authenticated-compojure-api.middleware.token-auth :refer [token-auth-mw]]
            [authenticated-compojure-api.route-functions.user.create-user :refer [create-user-response]]
            [authenticated-compojure-api.route-functions.user.delete-user :refer [delete-user-response]]
            [authenticated-compojure-api.route-functions.user.modify-user :refer [modify-user-response]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [compojure.api.sweet :refer :all]))


(defroutes* user-routes
  (context "/api" []

    (POST* "/user"      {:as request}
           :tags        ["User"]
           :return      {:username String}
           :middlewares [cors-mw]
           :body-params [email :- String username :- String password :- String]
           :summary     "Create a new user with provided username, email and password."
           (create-user-response email username password))

    (wrap-authentication
     (DELETE* "/user/:id"  {:as request}
              :tags        ["User"]
              :path-params [id :- Long]
              :return      {:message String}
              :middlewares [cors-mw token-auth-mw]
              :summary     "Deletes the specified user. Requires token to have `admin` auth or self ID."
              :description "Authorization header expects the following format 'Token {token}'"
              (delete-user-response request id))
     token-backend)

    (wrap-authentication
     (PATCH*  "/user/:id"    {:as request}
              :tags          ["User"]
              :path-params   [id :- Long]
              :body-params   [{username :- String ""} {password :- String ""} {email :- String ""}]
              :header-params [authorization :- String]
              :return        {:id Long :email String :username String}
              :middlewares   [cors-mw token-auth-mw]
              :summary       "Update some or all fields of a specified user. Requires token to have `admin` auth or self ID."
              :description   "Authorization header expects the following format 'Token {token}'"
              (modify-user-response request id username password email))
     token-backend)))
