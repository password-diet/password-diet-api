(ns authenticated-compojure-api.routes.refresh-token
  (:require [authenticated-compojure-api.middleware.cors :refer [cors-mw]]
            [authenticated-compojure-api.route-functions.refresh-token.delete-refresh-token :refer [remove-refresh-token-response]]
            [authenticated-compojure-api.route-functions.refresh-token.gen-new-token :refer [gen-new-token-response]]
            [compojure.api.sweet :refer :all]))


(defroutes* refresh-token-routes
  (context "/api" []

    (GET* "/refresh-token/:refreshToken" []
          :tags            ["Refresh-Token"]
          :return          {:token String :refreshToken String}
          :path-params     [refreshToken :- String]
          :middlewares     [cors-mw]
          :summary         "Get a fresh token and new refresh-token with a valid refresh-token."
          (gen-new-token-response refreshToken))

    (DELETE* "/refresh-token/:refreshToken" []
             :tags            ["Refresh-Token"]
             :return          {:message String}
             :path-params     [refreshToken :- String]
             :middlewares     [cors-mw]
             :summary         "Delete the specific refresh-token"
             (remove-refresh-token-response refreshToken))))
