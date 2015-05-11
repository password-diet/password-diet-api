(ns password_diet_api.routes.home
  (:require [compojure.core :refer :all]
            [password_diet_api.views.layout :as layout]))

(defn home []
  (layout/common [:h1 "Hello World!"]))

(defroutes home-routes
  (GET "/" [] (home)))
