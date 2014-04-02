(ns om-todo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-todo.commands :as c]
            [cljs-uuid-utils]
            [cljs.core.async :refer [<! chan put! sliding-buffer]]))

(enable-console-print!)

(defn apply-command [app-state command]
  (om/transact! app-state [:commands ] #(conj % command))
  (om/transact! app-state [:lists ] #(c/apply-command command %)))

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

(defn event-val [e]
  (.. e -target -value))

(defn new-todo-input [list owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [command-chan]}]
      (let [id (:id list)]
        (dom/div nil
          (dom/input #js
            {:type "text"
             :onChange #(om/set-state! owner :input-val (event-val %))})
          (dom/button #js
            {:onClick #(put! command-chan
                         (c/CreateTodoCommand.
                           (make-uuid)
                           (om/get-state owner [:input-val ])
                           id
                           (current-timestamp)))}
            "Add"))))))

(defn list-view [app owner {:keys [list-id] :as params}]
  (reify
    om/IRenderState
    (render-state [_ {:keys [command-chan] :as state}]
      (let [list (get (:lists app) list-id)
            todos (:todos list)]
        (dom/div nil
          (dom/p nil "Lists")
          (apply dom/ul nil
            (map (fn [list]
                   (let [id (:id list)]
                     (dom/li nil
                       (dom/a
                         #js {:href (str "#" (:name list))
                              :onClick #(om/update! app [:view :params :list-id ] id)}
                         (:name list)))))
              (vals (:lists app))))
          (dom/h2 nil (:name list))
          (om/build new-todo-input list {:init-state state})
          (apply dom/ol nil
            (map #(dom/li nil (str (:name %))) todos)))))))

(defn router [app owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (om/build list-view app {:init-state state :opts (-> app :view :params )}))))

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

(def app-state
  (atom {:text "Hello world!"
         :user {:username "dackerman"
                :name "David Ackerman"}
         :view {:name :list
                :params {:list-id 4}}
         :lists (recalc-app-state commands)
         :commands commands}))

(defn todos-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:command-chan (chan (sliding-buffer 100))})
    om/IWillMount
    (will-mount [_]
      (let [commands (om/get-state owner [:command-chan ])]
        (go (while true
              (let [command (<! commands)]
                ;(.log js/console (str "Received command: " (pr-str command)))
                (apply-command app command))))))
    om/IRenderState
    (render-state [this state]
      (om/build router app {:init-state state}))))

(om/root
  todos-view
  app-state
  {:target (. js/document (getElementById "app"))})
