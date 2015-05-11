(ns password_diet_api.views.layout
  (:require [hiccup.page :refer [html5 include-css]]))

(defn common [& body]
  (html5
    [:head
     [:title "Welcome to password_diet_api"]
     (include-css "/css/screen.css")]
    [:body body]))
