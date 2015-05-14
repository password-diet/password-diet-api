(ns authenticated-compojure-api.general-functions.user.create-token
   (:require [environ.core :refer [env]]
             [clj-time.core :as time]
             [buddy.sign.jws :as jws]))

(defn create-token [user]
  (let [stringify-user (-> user
                           (update-in [:username] str)
                           (update-in [:email] str)
                           (assoc     :exp (time/plus (time/now) (time/seconds 900))))
        token-contents (select-keys stringify-user [:permissions :username :email :id :exp])]
    (jws/sign token-contents (env :auth-key) {:alg :hs512})))
