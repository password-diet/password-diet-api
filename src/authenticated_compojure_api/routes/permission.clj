(ns authenticated-compojure-api.routes.permission
  (:require [authenticated-compojure-api.auth-resources.token-auth-backend :refer [token-backend]]
            [authenticated-compojure-api.middleware.cors :refer [cors-mw]]
            [authenticated-compojure-api.middleware.token-auth :refer [token-auth-mw]]
            [authenticated-compojure-api.route-functions.permission.add-user-permission :refer [add-user-permission-response]]
            [authenticated-compojure-api.route-functions.permission.delete-user-permission :refer [delete-user-permission-response]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [compojure.api.sweet :refer :all]))


(defroutes* permission-routes
  (context "/api" []

    (wrap-authentication
     (POST* "/permission/user/:id" {:as request}
            :tags                ["Permission"]
            :path-params         [id :- Long]
            :body-params         [permission :- String]
            :header-params       [authorization :- String]
            :return              {:message String}
            :middlewares         [cors-mw token-auth-mw]
            :summary             "Adds the specified permission for the specified user. Requires token to have `admin` auth."
            :description         "Authorization header expects the following format 'Token {token}'"
            (add-user-permission-response request id permission))
     token-backend)

    (wrap-authentication
     (DELETE* "/permission/user/:id" {:as request}
              :tags                  ["Permission"]
              :path-params           [id :- Long]
              :body-params           [permission :- String]
              :header-params         [authorization :- String]
              :return                {:message String}
              :middlewares           [cors-mw token-auth-mw]
              :summary               "Deletes the specified permission for the specified user. Requires token to have `admin` auth."
              :description           "Authorization header expects the following format 'Token {token}'"
              (delete-user-permission-response request id permission))
     token-backend)))
