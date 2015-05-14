(ns authenticated-compojure-api.route-functions.user.modify-user
  (:require [authenticated-compojure-api.queries.query-defs :as query]
            [buddy.hashers :as hashers]
            [ring.util.http-response :as respond]))

(defn modify-user [current-user-info username password email]
  (let [new-email     (if (empty? email)    (str (:email current-user-info)) email)
        new-username  (if (empty? username) (str (:username current-user-info)) username)
        new-password  (if (empty? password) (:password current-user-info) (hashers/encrypt password))
        new-user-info (query/update-registered-user<! {:id (:id current-user-info)
                                                       :email new-email
                                                       :username new-username
                                                       :password new-password
                                                       :refresh_token (:refresh_token current-user-info)})]
    (respond/ok {:id (:id current-user-info) :email new-email :username new-username})))

(defn modify-user-response [request id username password email]
  (let [auth              (get-in request [:identity :permissions])
        current-user-info (first (query/get-registered-user-by-id {:id id}))
        admin?            (.contains auth "admin")
        modifying-self?   (= id (get-in request [:identity :id]))
        admin-or-self?    (or admin? modifying-self?)
        modify?           (and admin-or-self? (not-empty current-user-info))]
    (cond
      modify?                    (modify-user current-user-info username password email)
      (not admin?)               (respond/unauthorized {:error "Not authorized"})
      (empty? current-user-info) (respond/not-found {:error "Userid does not exist"}))))

