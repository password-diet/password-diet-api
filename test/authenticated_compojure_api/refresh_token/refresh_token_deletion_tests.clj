(ns authenticated-compojure-api.refresh-token.refresh-token-deletion-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]))


(defn add-user []
  (let [user-map {:email "J@man.com" :username "JarrodCTaylor" :password "pass"}]
    (app (-> (mock/request :post "/api/user" (ch/generate-string user-map))
             (mock/content-type "application/json")))))

(defn setup-teardown [f]
  (try
    (query/create-registered-user-table-if-not-exists!)
    (query/create-permission-table-if-not-exists!)
    (query/create-user-permission-table-if-not-exists!)
    (query/insert-permission<! {:permission "basic"})
    (add-user)
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest can-delete-refresh-token-with-valid-refresh-token
  (testing "Can delete refresh token with valid refresh token"
    (let [initial-response         (app (-> (mock/request :get "/api/auth")
                                            (helper/basic-auth-header "JarrodCTaylor:pass")))
          initial-body             (helper/parse-body (:body initial-response))
          refresh-token            (:refreshToken initial-body)
          refresh-delete-response  (app (mock/request :delete (str "/api/refresh-token/" refresh-token)))
          body                     (helper/parse-body (:body refresh-delete-response))
          registered-user-row      (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200 (:status refresh-delete-response)))
      (is (= "Refresh token successfully deleted" (:message body)))
      (is (= nil (:refresh_token registered-user-row))))))

(deftest attempting-to-delete-an-invalid-refresh-token-returns-an-error
  (testing "Attempting to delete an invalid refresh token returns an error"
    (let [refresh-delete-response  (app (mock/request :delete (str "/api/refresh-token/" "123abc")))
          body                     (helper/parse-body (:body refresh-delete-response))]
      (is (= 404 (:status refresh-delete-response)))
      (is (= "The refresh token does not exist" (:error body))))))
