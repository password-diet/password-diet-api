(ns authenticated-compojure-api.auth.credential-retrieval-tests
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.sign.jws :as jws]
            [cheshire.core :as ch]
            [ring.mock.request :as mock]))

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
    (add-users)
    (f)
    (finally
      (query/drop-user-permission-table!)
      (query/drop-permission-table!)
      (query/drop-registered-user-table!))))

(use-fixtures :each setup-teardown)

(deftest valid-username-and-password-return-correct-auth-credentials
  (testing "Valid username and password return correct auth credentials"
    (let [response       (app (-> (mock/request :get "/api/auth")
                                  (helper/basic-auth-header "Everyman:pass")))
          body           (helper/parse-body (:body response))
          token-contents (jws/unsign (:token body) (env :auth-key) {:alg :hs512})]
      (is (= 5           (count body)))
      (is (= 200         (:status response)))
      (is (= "Everyman"  (:username body)))
      (is (= 1           (:id body)))
      (is (= "basic"     (:permissions body)))
      (is (= 36          (count (:refreshToken body))))
      (is (= 5           (count        token-contents)))
      (is (= "basic"     (:permissions token-contents)))
      (is (= 1           (:id          token-contents)))
      (is (= "e@man.com" (:email       token-contents)))
      (is (= "Everyman"  (:username    token-contents)))
      (is (number?       (:exp         token-contents))))))

(deftest valid-email-and-password-return-correct-auth-credentials
  (testing "Valid email and password return correct auth credentials"
    (let [response       (app (-> (mock/request :get "/api/auth")
                                  (helper/basic-auth-header "e@man.com:pass")))
          body           (helper/parse-body (:body response))
          token-contents (jws/unsign (:token body) (env :auth-key) {:alg :hs512})]
      (is (= 5           (count body)))
      (is (= 200         (:status response)))
      (is (= "Everyman"  (:username body)))
      (is (= 1           (:id body)))
      (is (= "basic"     (:permissions body)))
      (is (= 36          (count (:refreshToken body))))
      (is (= 5           (count        token-contents)))
      (is (= "basic"     (:permissions token-contents)))
      (is (= 1           (:id          token-contents)))
      (is (= "e@man.com" (:email       token-contents)))
      (is (= "Everyman"  (:username    token-contents)))
      (is (number?       (:exp         token-contents))))))

(deftest mutiple-permissions-are-properly-formated
  (testing "Multiple permissions are properly formated"
    (query/insert-permission<! {:permission "admin"})
    (query/insert-permission-for-user<! {:userid 2 :permission "admin"})
    (let [response (app (-> (mock/request :get "/api/auth")
                            (helper/basic-auth-header "JarrodCTaylor:pass")))
          body     (helper/parse-body (:body response))]
      (is (= 200             (:status response)))
      (is (= "JarrodCTaylor" (:username body)))
      (is (= 36              (count (:refreshToken body))))
      (is (= "basic,admin"   (:permissions (jws/unsign (:token body) (env :auth-key) {:alg :hs512})))))))

(deftest invlid-password-does-not-return-auth-credentials
  (testing "Invalid password does not return auth credentials"
    (let [response (app (-> (mock/request :get "/api/auth")
                            (helper/basic-auth-header "JarrodCTaylor:badpass")))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body))))))

(deftest invlid-username-does-not-return-auth-credentials
  (testing "Invalid username does not return auth credentials"
    (let [response (app (-> (mock/request :get "/api/auth")
                            (helper/basic-auth-header "baduser:badpass")))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body))))))

(deftest no-auth-credentials-are-returned-when-no-username-and-password-provided
  (testing "No auth credentials are returned when no username and password provided"
    (let [response (app (mock/request :get "/api/auth"))
          body     (helper/parse-body (:body response))]
      (is (= 401              (:status response)))
      (is (= "Not authorized" (:error body))))))

(deftest user-can-generate-a-new-token-with-a-valid-refresh-token
  (testing "User can generate a new tokens with a valid refresh-token"
    (let [initial-response   (app (-> (mock/request :get "/api/auth")
                                      (helper/basic-auth-header "JarrodCTaylor:pass")))
          initial-body       (helper/parse-body (:body initial-response))
          refresh-token      (:refreshToken initial-body)
          refreshed-response (app (mock/request :get (str "/api/refresh-token/" refresh-token)))
          body               (helper/parse-body (:body refreshed-response))
          token-contents     (jws/unsign (:token body) (env :auth-key) {:alg :hs512})]
      (is (= 200              (:status refreshed-response)))
      (is (= 2                (count body)))
      (is (= true             (contains? body :token)))
      (is (= true             (contains? body :refreshToken)))
      (is (not= refresh-token (:refreshToken body)))
      (is (= 5                (count        token-contents)))
      (is (= "basic"          (:permissions token-contents)))
      (is (= 2                (:id          token-contents)))
      (is (= "J@man.com"      (:email       token-contents)))
      (is (= "JarrodCTaylor"  (:username    token-contents)))
      (is (number?            (:exp         token-contents))))))

(deftest invalid-refresh-token-does-not-return-a-new-token
  (testing "Invalid refresh token does not return a new token"
    (let [response       (app (mock/request :get "/api/refresh-token/abcd1234"))
          body           (helper/parse-body (:body response))]
      (is (= 400           (:status response)))
      (is (= "Bad Request" (:error body))))))
