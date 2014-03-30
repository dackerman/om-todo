(ns om-todo.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-uuid-utils]))

(enable-console-print!)

(defn make-uuid []
  (cljs-uuid-utils/make-random-uuid))

(defn current-timestamp []
  (.getTime (js/Date.)))

;; Command creation functions
(defn create-todo-command [id name list-id]
  {:type :create-todo
   :name name
   :list list-id
   :id id
   :timestamp (current-timestamp)})

(defn create-list-command [id name]
  {:type :create-list
   :name name
   :id id
   :timestamp (current-timestamp)})

;; Object creation/modification functions
(defn make-list [command]
  {:name (:name command)
   :id (:id command)})

(defn make-todo [command]
  {:name (:name command)
   :id (:id command)
   :list (:list command)})

(defn recalc-app-state [all-commands]
  (loop [lists []
         todos []
         commands all-commands]
    (if (empty? commands)
      [lists todos]
      (let [command (first commands)]
        (condp = (:type command)
          :create-list
          (recur (conj lists (make-list command)) todos (rest commands))
          :create-todo
          (recur lists (conj todos (make-todo command)) (rest commands)))))))

(defn list-with-id [lists id]
  (first (filter #(= (:id %) id) lists)))

(defn todos-for-list [todos list-id]
  (filter #(= (:list %) list-id) todos))

(defn list-view [app owner {:keys [list-id] :as params}]
  (reify
    om/IRender
    (render [_]
      (let [list (list-with-id (:lists app) list-id)
            todos (todos-for-list (:todos app) list-id)]
        (dom/div nil
          (dom/h2 nil (:name list))
          (apply dom/ol nil
            (map #(dom/li nil (:name %)) todos)))))))


(defn router [app owner]
  (reify
    om/IRender
    (render [_]
      (let [[lists todos] (recalc-app-state (:commands app))]
        (om/transact! app :lists (fn [_] lists))
        (om/transact! app :todos (fn [_] todos))
        (om/build list-view app {:opts (-> app :view :params)})))))


(defn page-view [app owner]
  (reify
    om/IRender
    (render [_]
      (let [[lists todos] (recalc-app-state (:commands app))]
        (.log js/console "lists: " (pr-str lists))
        (.log js/console "todos: " (pr-str todos))
        (dom/div nil
          (dom/h2 nil "Lists")
          (apply dom/ul nil
            (map
              #(dom/li nil (:name %))
              lists))
          (dom/h2 nil "Todos")
          (apply dom/ul nil
            (map
              #(dom/li nil (:name %))
              todos)))))))


(def app-state
  (atom {:text "Hello world!"
         :user {:username "dackerman"
                :name "David Ackerman"}
         :view {:name :list
                :params {:list-id 1}}
         :lists []
         :todos []
         :commands [(create-list-command 1 "Chores")
                    (create-todo-command 2 "Laundry" 1)
                    (create-todo-command 3 "Vacuum" 1)
                    (create-list-command 4 "Safeway")
                    (create-todo-command 5 "Almond Milk" 4)
                    (create-todo-command 5 "Safeway Chicken" 4)
                    (create-todo-command 5 "Lettuce" 4)
                    (create-todo-command 3 "Dishes" 1)]}))

(om/root
  router
  app-state
  {:target (. js/document (getElementById "app"))})
