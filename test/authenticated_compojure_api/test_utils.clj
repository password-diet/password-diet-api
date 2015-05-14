(ns authenticated-compojure-api.test-utils
  (:require [cheshire.core :as cheshire]
            [ring.mock.request :as mock]
            [authenticated-compojure-api.handler :refer [app]]
            [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.core.codecs :refer [str->base64]]))

(defn parse-body [body]
  (cheshire/parse-string (slurp body) true))

(defn basic-auth-header
  [request original]
  (mock/header request "Authorization" (str "Basic " (str->base64 original))))

(defn token-auth-header
  [request token]
  (mock/header request "Authorization" (str "Token " token)))

(defn get-user-token [username-and-password]
  (let [initial-response (app (-> (mock/request :get "/api/auth")
                                  (basic-auth-header username-and-password)))
        initial-body     (parse-body (:body initial-response))]
    (:token initial-body)))

(defn get-token-auth-header-for-user [request username-and-password]
  (token-auth-header request (get-user-token username-and-password)))

(defn get-permissions-for-user [id]
  (:permissions (first (query/get-permissions-for-userid {:userid id}))))
