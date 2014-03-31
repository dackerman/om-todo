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

(defn reorder-todo-command [todo-id new-location]
  {:type :reorder-todo
   :todo-id todo-id
   :new-location new-location})

;; Object creation/modification functions
(defn make-list [command]
  {:name (:name command)
   :id (:id command)})

(defn make-todo [command sort-pos]
  {:name (:name command)
   :id (:id command)
   :sort-pos sort-pos
   :list (:list command)})

(defn list-with-id [lists id]
  (first (filter #(= (:id %) id) lists)))

(defn todos-for-list [todos list-id]
  (filter #(= (:list %) list-id) todos))

(defn find-loc [lists list-id]
  (key (first (filter #(= (-> % val :id) list-id) (zipmap (iterate inc 0) lists)))))

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
          (let [list-id (:list command)
                loc-in-lists (find-loc lists (:list command))
                list (get lists loc-in-lists)
                sort-pos (:next-todo-id list 1)]
            (recur
              (assoc-in lists [loc-in-lists] (assoc list :next-todo-id (inc sort-pos)))
              (conj todos (make-todo command sort-pos))
              (rest commands)))
          :reorder-todo
          (recur lists todos (rest commands)))))))

(defn list-view [app owner {:keys [list-id] :as params}]
  (reify
    om/IRender
    (render [_]
      (let [list (list-with-id (:lists app) list-id)
            todos (todos-for-list (:todos app) list-id)]
        (dom/div nil
          (dom/h2 nil (:name list))
          (apply dom/ol nil
            (map #(dom/li nil (str (:name %) " (" (:sort-pos %) ")")) todos)))))))


(defn router [app owner]
  (reify
    om/IRender
    (render [_]
      (om/build list-view app {:opts (-> app :view :params)}))))


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
              #(dom/li nil (str (:name %) (:sort-pos %)))
              todos)))))))

(def app-state (atom nil))

(def commands [(create-list-command 1 "Chores")
               (create-todo-command 2 "Laundry" 1)
               (create-todo-command 3 "Vacuum" 1)
               (create-list-command 4 "Safeway")
               (create-todo-command 5 "Almond Milk" 4)
               (create-todo-command 6 "Safeway Chicken" 4)
               (create-todo-command 7 "Lettuce" 4)
               (create-todo-command 8 "Dishes" 1)
               (reorder-todo-command 8 0)])

;; Apply commands
(let [[lists todos] (recalc-app-state commands)]
  (.log js/console "lists: " (pr-str lists))
  (.log js/console "todos: " (pr-str todos))
  (swap!
    app-state
    (fn [_]
      {:text "Hello world!"
       :user {:username "dackerman"
              :name "David Ackerman"}
       :view {:name :list
              :params {:list-id 1}}
       :lists lists
       :todos todos
       :commands commands})))

(om/root
  router
  app-state
  {:target (. js/document (getElementById "app"))})
