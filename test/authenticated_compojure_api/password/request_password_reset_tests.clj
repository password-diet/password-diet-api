(ns authenticated-compojure-api.password.request-password-reset-tests
  (:require [clojure.test :refer :all]
            [authenticated-compojure-api.handler :refer :all]
            [authenticated-compojure-api.test-utils :as helper]
            [authenticated-compojure-api.queries.query-defs :as query]
            [authenticated-compojure-api.route-functions.password.request-password-reset :as unit-test]
            [ring.mock.request :as mock]
            [cheshire.core :as ch]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(def example-user {:email "Jarrod@JarrodCTaylor.com" :username "JarrodCTaylor" :password "pass"})

(defn add-users []
  (app (-> (mock/request :post "/api/user" (ch/generate-string example-user))
           (mock/content-type "application/json"))))

(defn gen-reset-json [email]
  (ch/generate-string {:userEmail        email
                       :fromEmail        "admin@something.com"
                       :subject          "Password reset"
                       :emailBodyPlain   "Here is your link.\nThanks,"
                       :responseBaseLink "http://something/reset"}))

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

(deftest test-add-response-link-to-html-body-returns-desired-string
  (testing "test add response link to html body returns desired string"
    (let [body           "<html><body><p>Hello There</p></body></html>"
          response-link  "http://somesite/reset/234"
          body-with-link (unit-test/add-response-link-to-html-body body response-link)]
      (is (= "<html><body><p>Hello There</p><br><p>http://somesite/reset/234</p></body></html>" body-with-link)))))

(deftest test-add-response-link-to-plain-body-returns-desired-string
  (testing "Test add response link to plain body reutrns desired string"
    (let [body           "Hello there"
          response-link  "http://somesite/reset/123"
          body-with-link (unit-test/add-response-link-to-plain-body body response-link)]
      (is (= "Hello there\n\nhttp://somesite/reset/123" body-with-link)))))

(deftest successfully-request-password-reset-with-email-for-a-valid-registered-user
  (testing "Successfully request password reset with email for a valid registered user"
    (with-redefs [unit-test/send-reset-email (fn [to-email from-email subject html-body plain-body] nil)]
      (let [reset-info-json  (gen-reset-json "Jarrod@JarrodCTaylor.com")
            response         (app (-> (mock/request :post "/api/password/reset-request" reset-info-json)
                                      (mock/content-type "application/json")))
            body             (helper/parse-body (:body response))
            pass-reset-row   (query/get-password-reset-keys-for-userid {:userid 1})
            pass-reset-key   (:reset_key (first pass-reset-row))
            valid-until-ts   (:valid_until (first pass-reset-row))
            ; shave off the last four digits so we can compare
            valid-until-str  (subs (str (c/to-long (c/from-sql-time valid-until-ts))) 0 8)
            one-day-from-now (subs (str (c/to-long (t/plus (t/now) (t/hours 24)))) 0 8)]
        (is (= 200                                                         (:status response)))
        (is (= 1                                                           (count pass-reset-row)))
        (is (= valid-until-str                                             one-day-from-now))
        (is (= "Reset email successfully sent to Jarrod@JarrodCTaylor.com" (:message body)))))))

(deftest invalid-user-email-return-404-when-requesting-password-reset
  (testing "Invalid user email returns 404 when requesting password reset"
    (let [reset-info-json (gen-reset-json "J@jrock.com")
          response        (app (-> (mock/request :post "/api/password/reset-request" reset-info-json)
                                   (mock/content-type "application/json")))
          body            (helper/parse-body (:body response))]
      (is (= 404                                         (:status response)))
      (is (= "No user exists with the email J@jrock.com" (:error body))))))
