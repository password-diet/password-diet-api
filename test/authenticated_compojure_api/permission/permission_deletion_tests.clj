(ns authenticated-compojure-api.permission.permission-deletion-tests
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

(deftest can-delete-user-permission-with-valid-token-and-admin-permissions
  (testing "Can delete user permission with valid token and admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (query/insert-permission-for-user<! {:userid 1 :permission "other"})
    (is (= "basic,other" (:permissions (first (query/get-permissions-for-userid {:userid 1})))))
    (let [response (app (-> (mock/request :delete "/api/permission/user/1"  (ch/generate-string {:permission "other"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 200                                                  (:status response)))
      (is (= "basic"                                              (helper/get-permissions-for-user 1)))
      (is (= "Permission 'other' for user 1 successfully removed" (:message body))))))

(deftest can-not-delete-user-permission-with-valid-token-and-no-admin-permissions
  (testing "Can not delete user permission with valid token and no admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (is (= "basic,admin" (:permissions (first (query/get-permissions-for-userid {:userid 2})))))
    (let [response (app (-> (mock/request :delete "/api/permission/user/2"  (ch/generate-string {:permission "other"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "Everyman:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body)))
      (is (= "basic,admin"    (helper/get-permissions-for-user 2))))))
