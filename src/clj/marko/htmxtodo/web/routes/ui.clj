(ns marko.htmxtodo.web.routes.ui
  (:require
   [marko.htmxtodo.web.middleware.exception :as exception]
   [marko.htmxtodo.web.routes.utils :as utils]
   [marko.htmxtodo.web.htmx :refer [ui] :as htmx]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn home [request]
  (ui
   "<!DOCTYPE html>"
   [:html
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "Htmx + Kit"]
     [:link
      {:href "https://unpkg.com/todomvc-app-css@2.4.1/index.css"
       :rel "stylesheet"}]
     [:script
      {:src "https://unpkg.com/htmx.org@1.7.0/dist/htmx.min.js" :defer true}]
     [:script
      {:src "https://unpkg.com/hyperscript.org@0.9.5/dist/_hyperscript.min.js"
       :defer true}]]
    [:body
     [:section.todoapp
      [:headerr.header
       [:h1 "todos"]
       [:p "todos form"]
       [:p "new todo 2"]]]
     [:section.main
      [:p "main part"]]
     [:ui#todo-list.todo-list
      [:p "todo list"]]
     [:footer.footer
      [:p "item count"]
      [:p "todo filters"]
      [:p "clear completed button"]]
     [:footer.info
      [:p "Click to edit a todo"]
      [:p "Created by "
       [:a {:href "https://marko.euptera.com"} "Marko Kocic"]]
      [:p "Inspired by "
       [:a {:href "https://twitter.com/PrestanceDesign"} "Michaël Sλlihi"]]
      [:p "Part of "
       [:a {:href "http://todomvc.com"} "TodoMVC"]]]]]))

(defn clicked [request]
  (ui
   [:div "Congratulations! You just clicked the button!"]))

;; Routes
(defn ui-routes [_opts]
  [["/" {:get #(home %)}]
   ["/clicked" {:post clicked}]])

(defn route-data [opts]
  (merge
   opts
   {:middleware 
    [;; Default middleware for ui
     ;; query-params & form-params
     parameters/parameters-middleware
     ;; encoding response body
     muuntaja/format-response-middleware
     ;; exception handling
     exception/wrap-exception]}))

(derive :reitit.routes/ui :reitit/routes)

(defmethod ig/init-key :reitit.routes/ui
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path (route-data opts) (ui-routes opts)])
