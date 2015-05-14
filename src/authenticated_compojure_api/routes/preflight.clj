(ns authenticated-compojure-api.routes.preflight
  (:require [authenticated-compojure-api.middleware.cors :refer [cors-mw]]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :as respond]))

(defroutes* preflight-route
  (context "/api" []

    (OPTIONS* "*"      {:as request}
              :tags        ["Preflight"]
              :return      {}
              :middlewares [cors-mw]
              :summary     "This will catch all OPTIONS preflight requests from the
                            browser. It will always return a success for the purpose
                            of the browser retrieving the response headers to validate cors
                            requests. For some reason it does not work in the swagger UI."
              (respond/ok  {}))))
