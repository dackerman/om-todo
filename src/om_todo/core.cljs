(ns om-todo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-todo.commands :as c]
            [cljs-uuid-utils]
            [cljs.core.async :refer [<! chan put! sliding-buffer]]))

(enable-console-print!)

(defn make-uuid []
  (cljs-uuid-utils/make-random-uuid))

(defn current-timestamp []
  ;(.getTime (js/Date.)))
  (js/moment))

(def due-today current-timestamp)

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
                           (due-today)
                           id
                           (current-timestamp)))}
            "Add"))))))

(defn date-picker [date-field owner]
  (reify
    om/IRenderState
    (render-state [_ _]
      (dom/span nil (.format date-field)))))

(defn edit-todo-view [todo owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
        #js {:className "edit-todo"}
        (dom/h2 nil (:name todo))
        (dom/hr nil nil)
        (dom/table nil
          (dom/tr nil
            (dom/td nil "Date")
            (dom/td nil (om/build date-picker (:due-date todo)))))))))

(defn debug-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/pre nil
        (dom/h4 nil "Lists")
        (apply dom/p nil
          (map (fn [list]
                 (dom/p nil
                   (pr-str (dissoc list :todos ))
                   (apply dom/ul nil
                     (map (fn [todo]
                            (dom/li nil (pr-str todo)))
                       (:todos list)))))
            (vals (:lists app))))
        (dom/h4 nil "Commands")
        (apply dom/p nil
          (map (fn [command]
                 (dom/p nil (pr-str command)))
            (:commands app)))))))

(defn list-view [app owner {:keys [list-id] :as params}]
  (reify
    om/IRenderState
    (render-state [_ {:keys [command-chan] :as state}]
      (let [list (get (:lists app) list-id)
            todos (:todos list)]
        (dom/div nil
          (if-let [todo (om/get-state owner :editing-todo )]
            (om/build edit-todo-view todo {:react-key (c/get-key todo)}))
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
          (om/build new-todo-input list {:key :id :init-state state})
          (apply dom/ol nil
            (map (fn [todo] (dom/li
                              #js {:onClick #(om/set-state! owner :editing-todo todo)}
                              (str (:name todo)))) todos))
          (om/build debug-view app))))))

(defn router [app owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (om/build list-view app {:react-key (-> app :view :params :list-id )
                               :init-state state :opts (-> app :view :params )}))))

(defn apply-command [app-state command]
  (om/transact! app-state [:commands ] #(conj % command))
  (om/transact! app-state [:lists ] #(c/apply-command command %)))

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
                (.log js/console (str "Applying command: " (pr-str command)))
                (apply-command app command))))))
    om/IRenderState
    (render-state [this state]
      (om/build router app {:init-state state}))))

;; App bootstrapping

(defn recalc-app-state [all-commands]
  (loop [commands all-commands
         lists {}]
    (if (empty? commands)
      lists
      (recur (rest commands) (c/apply-command (first commands) lists)))))

(def commands [(c/CreateListCommand. 1 "Chores" (current-timestamp))
               (c/CreateTodoCommand. 2 "Laundry" (due-today) 1 (current-timestamp))
               (c/CreateTodoCommand. 3 "Vacuum" (due-today) 1 (current-timestamp))
               (c/CreateListCommand. 4 "Safeway" (current-timestamp))
               (c/CreateTodoCommand. 5 "Almond Milk" (due-today) 4 (current-timestamp))
               (c/CreateTodoCommand. 6 "Safeway Chicken" (due-today) 4 (current-timestamp))
               (c/CreateTodoCommand. 7 "Lettuce" (due-today) 4 (current-timestamp))
               (c/CreateTodoCommand. 8 "Dishes" (due-today) 1 (current-timestamp))
               (c/ReorderTodoCommand. 4 7 1)])

(def app-state
  (atom {:user {:username "dackerman"
                :name "David Ackerman"}
         :view {:name :list
                :params {:list-id 4}}
         :lists (recalc-app-state commands)
         :commands commands}))

(om/root
  todos-view
  app-state
  {:target (. js/document (getElementById "app"))})
