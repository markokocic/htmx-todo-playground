(ns marko.htmxtodo.web.routes.ui
  (:require
   [clojure.pprint :refer [cl-format] :as pp]
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

(defn todo-edit [id name]
  [:form {:hx-patch (str "/todos/update/" id)}
   [:input.edit {:type :tex
                 :name "name"
                 :value name}]])

(defn todo-list [todos]
  (for [todo todos]
    (todo-item (val todo))))

(defn item-count []
  (let [items-left (t/get-items-left)]
    [:span#todo-count.todo-count {:hx-swap-oob "true"}
     [:strong items-left] (cl-format nil " item~p " items-left) "left"]))

(defn clear-completed-button []
  [:button#clear-completed.clear-completed
   {:hx-delete "/todos"
    :hx-target "#todo-list"
    :hx-swap-oob "true"
    :hx-push-url "/"
    :class (when-not (pos? (t/todos-completed)) "hidden")}
   "Clear completed"])

;;
;; handlers
;;

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
      {:src "https://unpkg.com/hyperscript.org@0.9.5" :defer true}]]
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
      (item-count)
      [:span "todo filters"]
      (clear-completed-button)]
     [:footer.info
      [:p "Click to edit a todo"]
      [:p "Created by "
       [:a {:href "https://marko.euptera.com"} "Marko Kocic"]]
      [:p "Inspired by "
       [:a {:href "https://twitter.com/PrestanceDesign"} "Michaël Sλlihi"]]
      [:p "Part of "
       [:a {:href "http://todomvc.com"} "TodoMVC"]]]]]))

(defn add-item [{{name :todo} :params}]
  (let [todo (t/add-todo! name)]
    (ui (list (todo-item (val (last todo)))
              (item-count)))))

(defn patch-item [{{id :id} :path-params}]
  (let [todo (t/toggle-todo! id)]
    (ui (list (todo-item (get todo (Integer. id)))
              (item-count)
              (clear-completed-button)))))

(defn delete-item [{{id :id} :path-params}]
  (t/remove-todo! id)
  (ui (item-count)))

(defn edit-item [{{id :id} :path-params}]
  (let [{:keys [id name]} (get @t/todos (Integer. id))]
    (ui (todo-edit id name))))

(defn update-item [{{id :id} :path-params {name :name} :params}]
  (let [todo (t/update-todo! id name)]
    (ui (todo-item (get todo (Integer. id))))))

(defn clear-completed [_]
  (t/remove-all-completed-todo)
  (ui (list (todo-list @t/todos)
            (item-count)
            (clear-completed-button))))


;; Routes
(defn ui-routes [_opts]
  [["/"
    {:get #(home %)}]
   ["/todos"
    {:post #(add-item %)
     :delete #(clear-completed %)}]
   ["/todos/:id"
    {:patch #(patch-item %)
     :delete #(delete-item %)}]
   ["/todos/edit/:id"
    {:get #(edit-item %)}]
   ["/todos/update/:id"
    {:patch #(update-item %)}]])

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
