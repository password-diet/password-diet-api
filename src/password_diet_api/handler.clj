(ns password_diet_api.handler
  (:require [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [password_diet_api.routes.home :refer [home-routes]])
  (:use org.httpkit.server))



(defn init []
  (println "password_diet_api is starting"))

(defn destroy []
  (println "password_diet_api is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes app-routes)
      (handler/site)
      (wrap-base-url)))

(run-server app {:port 8080})
