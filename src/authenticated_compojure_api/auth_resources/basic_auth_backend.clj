(ns authenticated-compojure-api.auth-resources.basic-auth-backend
  (:require [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
            [buddy.hashers :as hashers]))

;; ============================================================================
;; The username and email values are stored in citext fields in Postgres thus
;; the need to convert them to strings for future use. Since we want to accept
;; eiter username or email as an identifier we will query for both and check
;; for a match.
;; ============================================================================
(defn get-user-info [identifier]
  (let [registered-user-username (first (query/get-registered-user-details-by-username {:username identifier}))
        registered-user-email    (first (query/get-registered-user-details-by-email {:email identifier}))
        registered-user          (first (remove nil? [registered-user-username registered-user-email]))]
    (println identifier)
    (println (query/get-registered-user-details-by-username {:username identifier}))
    (when-not (nil? registered-user)
      {:user-data (-> registered-user
                      (assoc-in [:username] (str (:username registered-user)))
                      (assoc-in [:email]    (str (:email registered-user)))
                      (dissoc   :password))
       :password  (:password registered-user)})))

;; ============================================================================
;  This function will delegate determining if we have the correct username and
;  password to authorize a user. The return value will be added to the request
;  with the keyword of :identity. We will accept either a valid username or
;  valid user email in the username field. It is a little strange but to adhere
;  to legacy basic auth api of using username:password we have to make the
;  field do double duty.
;; ============================================================================
(defn basic-auth [request, auth-data]
  (let [identifier  (:username auth-data)
        password    (:password auth-data)
        user-info   (get-user-info identifier)]
    (println identifier)
    (println password)
    (if (and user-info (hashers/check password (:password user-info)))
      (:user-data user-info)
      false)))

;; ============================================================================
;  Create authentication backend
;; ============================================================================
(def basic-backend (http-basic-backend {:authfn basic-auth}))
