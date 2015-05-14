(ns authenticated-compojure-api.user.user-modify-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer [app]]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.hashers :as hashers]
            [cheshire.core :as ch]
            [ring.mock.request :as mock]))

(def user-one {:email "some@guy.com" :username "SomeGuy"       :password "pass"})
(def user-two {:email "J@man.com"    :username "JarrodCTaylor" :password "pass"})

(defn add-users []
  (app (-> (mock/request :post "/api/user" (ch/generate-string user-one))
           (mock/content-type "application/json")))
  (app (-> (mock/request :post "/api/user" (ch/generate-string user-two))
           (mock/content-type "application/json"))))

(defn setup-teardown [f]
  (try
    (query/create-registered-user-table-if-not-exists!)
    (query/create-permission-table-if-not-exists!)
    (query/create-user-permission-table-if-not-exists!)
    (query/insert-permission<! {:permission "basic"})
    (query/insert-permission<! {:permission "admin"})
    (add-users)
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest can-modify-a-users-username-with-valid-token-and-admin-permissions
  (testing "Can modify a users username with valid token and admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (let [response  (app (-> (mock/request :patch "/api/user/1" (ch/generate-string {:username "Newman"}))
                             (mock/content-type "application/json")
                             (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body      (helper/parse-body (:body response))]
      (is (= 200            (:status response)))
      (is (= "Newman"       (:username body)))
      (is (= "some@guy.com" (:email body))))))

(deftest can-modify-a-users-email-with-valid-token-and-admin-permissions
  (testing "Can modify a users email with valid token and admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (let [response     (app (-> (mock/request :patch "/api/user/1" (ch/generate-string {:email "new@email.com"}))
                                (mock/content-type "application/json")
                                (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200             (:status response)))
      (is (= "SomeGuy"       (:username body)))
      (is (= "new@email.com" (:email body)))
      (is (= "new@email.com" (str (:email updated-user)))))))

(deftest can-modify-a-users-password-with-valid-token-and-admin-permissions
  (testing "Can modify a users password with valid token and admin permissions"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (let [response     (app (-> (mock/request :patch "/api/user/1" (ch/generate-string {:password "newPass"}))
                                (mock/content-type "application/json")
                                (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200  (:status response)))
      (is (= true (hashers/check "newPass" (:password updated-user)))))))

(deftest can-modify-your-own-password-with-valid-token-and-no-admin-permissions
  (testing "Can modify your own password with valid token and no admin permissions"
    (let [response     (app (-> (mock/request :patch "/api/user/1" (ch/generate-string {:password "newPass"}))
                                (mock/content-type "application/json")
                                (helper/get-token-auth-header-for-user "SomeGuy:pass")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200  (:status response)))
      (is (= true (hashers/check "newPass" (:password updated-user)))))))

(deftest can-not-modify-a-user-with-valid-token-and-no-admin-permissions
  (testing "Can not modify a user with valid token and no admin permissions"
    (let [response         (app (-> (mock/request :patch "/api/user/1" (ch/generate-string {:email "bad@mail.com"}))
                                    (mock/content-type "application/json")
                                    (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body             (helper/parse-body (:body response))
          non-updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 401              (:status response)))
      (is (= "some@guy.com"   (str (:email non-updated-user))))
      (is (= "Not authorized" (:error body))))))

(deftest trying-to-modify-a-user-that-does-not-exist-return-a-404
  (testing "Trying to modify a user that does not exist returns a 404"
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (let [response (app (-> (mock/request :patch "/api/user/99" (ch/generate-string {:email "not@real.com"}))
                            (mock/content-type "application/json")
                            (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 404                     (:status response)))
      (is (= "Userid does not exist" (:error body))))))
