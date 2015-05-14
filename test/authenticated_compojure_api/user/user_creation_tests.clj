(ns authenticated-compojure-api.user.user-creation-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :refer [parse-body]]
            [authenticated-compojure-api.queries.query-defs :as query]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]))

(defn create-user [user-map]
  (app (-> (mock/request :post "/api/user" (ch/generate-string user-map))
           (mock/content-type "application/json"))))

(defn assert-no-dup [user-1 user-2 expected-error-message]
  (let [_        (create-user user-1)
        response (create-user user-2)
        body     (parse-body (:body response))]
    (is (= 409                    (:status response)))
    (is (= 1                      (count (query/all-registered-users))))
    (is (= expected-error-message (:error body)))))

(defn setup-teardown [f]
  (try
    (query/create-registered-user-table-if-not-exists!)
    (query/create-permission-table-if-not-exists!)
    (query/create-user-permission-table-if-not-exists!)
    (query/insert-permission<! {:permission "basic"})
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest can-successfully-create-a-new-user-who-is-given-basic-permission-as-default
  (testing "Can successfully create a new user who is given basic permission as default"
    (is (= 0 (count (query/all-registered-users))))
    (let [response            (create-user {:email "J@Taylor.com" :username "Jarrod" :password "pass"})
          body                (parse-body (:body response))
          new-registered-user (first (query/get-registered-user-details-by-username {:username (:username body)}))]
      (is (= 201      (:status response)))
      (is (= 1        (count (query/all-registered-users))))
      (is (= "Jarrod" (:username body)))
      (is (= "Jarrod" (str (:username new-registered-user))))
      (is (= "basic"  (:permissions new-registered-user))))))

(deftest can-not-create-a-user-if-username-already-exists-using-the-same-case
  (testing "Can not create a user if username already exists using the same case"
    (assert-no-dup {:email "Jrock@Taylor.com" :username "Jarrod" :password "pass"}
                   {:email "J@Taylor.com"     :username "Jarrod" :password "pass"}
                   "Username already exists")))

(deftest can-not-create-a-user-if-username-already-exists-using-mixed-case
  (testing "Can not create a user if username already exists using mixed case"
    (assert-no-dup {:email "Jrock@Taylor.com" :username "jarrod" :password "pass"}
                   {:email "J@Taylor.com"     :username "Jarrod" :password "pass"}
                   "Username already exists")))

(deftest can-not-create-a-user-if-email-already-exists-using-the-same-case
  (testing "Can not create a user if email already exists using the same case"
    (assert-no-dup {:email "jarrod@taylor.com" :username "Jarrod"   :password "the-first-pass"}
                   {:email "jarrod@taylor.com" :username "JarrodCT" :password "the-second-pass"}
                   "Email already exists")))

(deftest can-not-create-a-user-if-email-already-exists-using-mixed-case
  (testing "Can not create a user if email already exists using mixed case"
    (assert-no-dup {:email "wOnkY@email.com" :username "Jarrod" :password "Pass"}
                   {:email "WonKy@email.com" :username "Jrock"  :password "Pass"}
                   "Email already exists")))

(deftest can-not-create-a-user-if-username-and-email-already-exist-using-same-and-mixed-case
  (testing "Can not create a user if username and email already exist using same and mixed case"
    (assert-no-dup {:email "wOnkY@email.com" :username "jarrod" :password "pass"}
                   {:email "WonKy@email.com" :username "jarrod" :password "pass"}
                   "Username and Email already exist")))
