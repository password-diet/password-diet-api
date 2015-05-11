(defproject password_diet_api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [ring-server "0.4.0"]
                 [http-kit "2.1.19"]
                 [mysql/mysql-connector-java "5.1.35"]
                 [yesql "0.4.1"]
                 [ragtime "0.3.8"]
                 [liberator "0.12.2"]
                 [buddy/buddy-core "0.5.0"]
                 [buddy/buddy-hashers "0.4.2"]
                 [buddy/buddy-auth "0.5.2"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler password_diet_api.handler/app
         :init password_diet_api.handler/init
         :destroy password_diet_api.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.2"]]}})
