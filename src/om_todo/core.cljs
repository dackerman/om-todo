(ns om-todo.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-uuid-utils]))

(enable-console-print!)

(defn make-uuid []
  (cljs-uuid-utils/make-random-uuid))

(defn current-timestamp []
  (.getTime (js/Date.)))

(defrecord Todo [id name list-id])

(defrecord TodoList [id name todos])

(defn add-todo [list todo]
  (update-in list [:todos] (fn [todos] (conj todos todo))))

(defprotocol Command
  (apply-command [this todos]))

(defrecord CreateListCommand [id name timestamp]
  Command
  (apply-command [this lists]
    (let [list (TodoList. id name [])]
      (assoc lists id list))))

(defrecord CreateTodoCommand [id name list-id timestamp]
  Command
  (apply-command [this lists]
    (let [todo (Todo. id name list-id)]
      (update-in lists [list-id] (fn [list] (add-todo list todo))))))

(defn recalc-app-state [all-commands]
  (loop [commands all-commands
         lists {}]
    (if (empty? commands)
      lists
      (recur (rest commands) (apply-command (first commands) lists)))))

(defn list-view [app owner {:keys [list-id] :as params}]
  (reify
    om/IRender
    (render [_]
      (let [list (get (:lists app) list-id)
            todos (:todos list)]
        (dom/div nil
          (dom/h2 nil (:name list))
          (apply dom/ol nil
            (map #(dom/li nil (str (:name %))) todos)))))))

(defn router [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build list-view app {:opts (-> app :view :params)}))))

(def app-state (atom nil))

(def commands [(CreateListCommand. 1 "Chores" (current-timestamp))
               (CreateTodoCommand. 2 "Laundry" 1 (current-timestamp))
               (CreateTodoCommand. 3 "Vacuum" 1 (current-timestamp))
               (CreateListCommand. 4 "Safeway" (current-timestamp))
               (CreateTodoCommand. 5 "Almond Milk" 4 (current-timestamp))
               (CreateTodoCommand. 6 "Safeway Chicken" 4 (current-timestamp))
               (CreateTodoCommand. 7 "Lettuce" 4 (current-timestamp))
               (CreateTodoCommand. 8 "Dishes" 1 (current-timestamp))])

;; Apply commands
(let [lists (recalc-app-state commands)]
  (.log js/console "lists: " (pr-str lists))
  (swap!
    app-state
    (fn [_]
      {:text "Hello world!"
       :user {:username "dackerman"
              :name "David Ackerman"}
       :view {:name :list
              :params {:list-id 1}}
       :lists lists
       :commands commands})))

(om/root
  router
  app-state
  {:target (. js/document (getElementById "app"))})
