(ns marko.htmxtodo.web.routes.ui
  (:require
   [marko.htmxtodo.web.middleware.exception :as exception]
   [marko.htmxtodo.web.routes.utils :as utils]
   [marko.htmxtodo.web.htmx :refer [ui] :as htmx]
   [marko.htmxtodo.web.controllers.todos :as t]
   [integrant.core :as ig]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]))

(defn todo-item [{:keys [id name done]}]
  [:li {:id (str "todo-" id)
        :class (when done "completed")}
   [:div.view
    [:input.toggle {:hx-patch (str "/todos/" id)
                    :type "checkbox"
                    :checked done
                    :hx-target (str "#todo-" id)
                    :hx-swap "outerHTML"}]
    [:label {:hx-get (str "/todos/edit/" id)
             :hx-target (str "#todo-" id)
             :hx-swap "outerHTML"} name]
    [:button.destroy {:hx-delete (str "/todos/" id)
                      :_ (str "on htmx:afterOnLoad remove #todo-" id)}]]])

(defn todo-list [todos]
  (for [todo todos]
    (todo-item (val todo))))

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
       [:form
        {:hx-post "/todos"
         :hx-target "#todo-list"
         :hx-swap "beforeend"
         :_ "on htmx:afterOnLoad set #txtTodo.value to ''"}
        [:input#txtTodo.new-todo
         {:name "todo"
          :placeholder "What needs to be done?"
          :autofocus ""}]]]]
     [:section.main
      [:input#toggle-all.toggle-all {:type "checkbox"}]
      [:label {:for "toggle-all"} "Mark all as complete"]]
     [:ul#todo-list.todo-list
      (todo-list @t/todos)]
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
