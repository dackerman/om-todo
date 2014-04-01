(ns om-todo.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-todo.commands :as c]
            [cljs-uuid-utils]))

(enable-console-print!)

(defn make-uuid []
  (cljs-uuid-utils/make-random-uuid))

(defn current-timestamp []
  (.getTime (js/Date.)))

(defn recalc-app-state [all-commands]
  (loop [commands all-commands
         lists {}]
    (if (empty? commands)
      lists
      (recur (rest commands) (c/apply-command (first commands) lists)))))

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

(def commands [(c/CreateListCommand. 1 "Chores" (current-timestamp))
               (c/CreateTodoCommand. 2 "Laundry" 1 (current-timestamp))
               (c/CreateTodoCommand. 3 "Vacuum" 1 (current-timestamp))
               (c/CreateListCommand. 4 "Safeway" (current-timestamp))
               (c/CreateTodoCommand. 5 "Almond Milk" 4 (current-timestamp))
               (c/CreateTodoCommand. 6 "Safeway Chicken" 4 (current-timestamp))
               (c/CreateTodoCommand. 7 "Lettuce" 4 (current-timestamp))
               (c/CreateTodoCommand. 8 "Dishes" 1 (current-timestamp))
               (c/ReorderTodoCommand. 4 7 1)])

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
              :params {:list-id 4}}
       :lists lists
       :commands commands})))

(om/root
  router
  app-state
  {:target (. js/document (getElementById "app"))})
