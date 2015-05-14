(ns authenticated-compojure-api.route-functions.password.request-password-reset
  (:require [authenticated-compojure-api.queries.query-defs :as query]
            [environ.core :refer [env]]
            [postal.core :refer [send-message]]
            [ring.util.http-response :as respond]))

(defn add-response-link-to-plain-body [body response-link]
  (str body "\n\n" response-link))

(defn add-response-link-to-html-body [body response-link]
  (let [body-less-closing-tags (clojure.string/replace body #"</body></html>" "")]
    (str body-less-closing-tags "<br><p>" response-link "</p></body></html>")))

(defn send-reset-email [to-email from-email subject html-body plain-body]
  (send-message {:host "smtp.mandrillapp.com"
                 :user (env :user-email)
                 :port 587
                 :pass (env :user-pass-key)}
                {:from from-email
                 :to to-email
                 :subject subject
                 :body [:alternative
                        {:type "text/plain" :content plain-body}
                        {:type "text/html"  :content html-body}]}))

(defn process-password-reset-request [user from-email subject email-body-plain email-body-html response-base-link]
  (let [reset-key     (str (java.util.UUID/randomUUID))
        the-insert    (query/insert-password-reset-key-with-default-valid-until<! {:reset_key reset-key :user_id (:id user)})
        response-link (str response-base-link "/" (:reset_key the-insert))
        body-plain    (add-response-link-to-plain-body email-body-plain response-link)
        body-html     (add-response-link-to-html-body email-body-html response-link)]
    (send-reset-email (str (:email user)) from-email subject body-html body-plain)
    (respond/ok {:message (str "Reset email successfully sent to " (str (:email user)))})))

(defn request-password-reset-response [user-email from-email subject email-body-plain email-body-html response-base-link]
  (let [user (first (query/get-registered-user-by-email {:email user-email}))]
    (if (empty? user)
      (respond/not-found {:error (str "No user exists with the email " user-email)})
      (process-password-reset-request user from-email subject email-body-plain email-body-html response-base-link))))
