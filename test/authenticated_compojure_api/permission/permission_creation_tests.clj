(ns authenticated-compojure-api.permission.permission-creation-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]))

(def basic-user {:email "e@man.com" :username "Everyman"      :password "pass"})
(def admin-user {:email "J@man.com" :username "JarrodCTaylor" :password "pass"})

(defn add-users []
  (app (-> (mock/request :post "/api/user" (ch/generate-string basic-user))
           (mock/content-type "application/json")))
  (app (-> (mock/request :post "/api/user" (ch/generate-string admin-user))
           (mock/content-type "application/json"))))

(defn setup-teardown [f]
  (try
    (query/create-registered-user-table-if-not-exists!)
    (query/create-permission-table-if-not-exists!)
    (query/create-user-permission-table-if-not-exists!)
    (query/insert-permission<! {:permission "basic"})
    (query/insert-permission<! {:permission "admin"})
    (query/insert-permission<! {:permission "other"})
    (add-users)
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest can-add-user-permission-with-valid-token-and-admin-permissions
  (testing "Can add user permission with valid token and admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid 1})))))
    (let [response (app (-> (mock/request :post "/api/permission/user/1" (ch/generate-string {:permission "other"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 200                                                (:status response)))
      (is (= "Permission 'other' for user 1 successfully added" (:message body)))
      (is (= "basic,other"                                      (helper/get-permissions-for-user 1))))))

(deftest attempting-to-add-a-permission-that-does-not-exist-returns-404
  (testing "Attempting to add a permission that does not exist returns 404"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid 1})))))
    (let [response (app (-> (mock/request :post "/api/permission/user/1" (ch/generate-string {:permission "stranger"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 404                                    (:status response)))
      (is (= "Permission 'stranger' does not exist" (:error body)))
      (is (= "basic"                                (helper/get-permissions-for-user 1))))))

(deftest can-not-add-user-permission-with-valid-token-and-no-admin-permissions
  (testing "Can not add user permission with valid token and no admin permissions"
    (is (= "basic" (:permissions (first (query/get-permissions-for-userid {:userid 2})))))
    (let [response (app (-> (mock/request :post "/api/permission/user/2"  (ch/generate-string {:permission "other"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "Everyman:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body)))
      (is (= "basic"          (helper/get-permissions-for-user 2))))))
