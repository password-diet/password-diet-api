(ns authenticated-compojure-api.password.password-reset-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.hashers :as hashers]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def example-user {:email "Jarrod@JarrodCTaylor.com" :username "JarrodCTaylor" :password "pass"})

(defn add-users []
  (app (-> (mock/request :post "/api/user" (ch/generate-string example-user))
           (mock/content-type "application/json"))))

(defn setup-teardown [f]
  (try
    (query/create-registered-user-table-if-not-exists!)
    (query/create-permission-table-if-not-exists!)
    (query/create-user-permission-table-if-not-exists!)
    (query/create-password-reset-key-table-if-not-exists!)
    (query/insert-permission<! {:permission "basic"})
    (add-users)
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-password-reset-key-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest test-password-is-reset-with-valid-reset-key
  (testing "Test password is reset with valid resetKey"
    (query/insert-password-reset-key-with-default-valid-until<! {:reset_key "123" :user_id 1})
    (let [response     (app (-> (mock/request :post "/api/password/reset-confirm" (ch/generate-string {:resetKey "123" :newPassword "new-pass"}))
                                (mock/content-type "application/json")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200 (:status response)))
      (is (= true (hashers/check "new-pass" (:password updated-user))))
      (is (= "Password successfully reset" (:message body))))))

(deftest not-found-404-is-returned-when-invlid-reset-key-id-used
  (testing "Not found 404 is returned when invalid reset key is used"
    (let [response     (app (-> (mock/request :post "/api/password/reset-confirm" (ch/generate-string {:resetKey "123" :newPassword "new-pass"}))
                                (mock/content-type "application/json")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 404 (:status response)))
      (is (= "Reset key does not exist" (:error body))))))

(deftest not-found-404-is-returned-when-valid-reset-key-has-expired
  (testing "Not found 404 is returned when valid reset key has expired"
    (query/insert-password-reset-key-with-provide-valid-until-date<! {:reset_key "123" :user_id 1 :valid_until (c/to-sql-time (t/minus (t/now) (t/hours 24)))})
    (let [response     (app (-> (mock/request :post "/api/password/reset-confirm" (ch/generate-string {:resetKey "123" :newPassword "new-pass"}))
                                (mock/content-type "application/json")))
          body         (helper/parse-body (:body response))
          updated-user (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 404 (:status response)))
      (is (= "Reset key has expired" (:error body))))))

(deftest password-is-not-reset-if-reset-key-has-already-been-used
  (testing "Password is not reset if reset key has already been used"
    (query/insert-password-reset-key-with-default-valid-until<! {:reset_key "123" :user_id 1})
    (let [initial-response (app (-> (mock/request :post "/api/password/reset-confirm" (ch/generate-string {:resetKey "123" :newPassword "new-pass"}))
                                    (mock/content-type "application/json")))
          second-response  (app (-> (mock/request :post "/api/password/reset-confirm" (ch/generate-string {:resetKey "123" :newPassword "nono"}))
                                    (mock/content-type "application/json")))
          body             (helper/parse-body (:body second-response))
          updated-user     (first (query/get-registered-user-by-id {:id 1}))]
      (is (= 200 (:status initial-response)))
      (is (= 404 (:status second-response)))
      (is (= true (hashers/check "new-pass" (:password updated-user))))
      (is (= "Reset key already used" (:error body))))))
