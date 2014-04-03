(ns om-todo.commands)

(defrecord Todo [id name list-id])

(defn get-key [todo]
  (pr-str (:id todo)))

(defrecord TodoList [id name todos])

(defn add-todo [list todo]
  (update-in list [:todos] (fn [todos] (conj todos todo))))

(defn splice [find-fn list new-location]
  (let [item (filter find-fn list)
        removed-list (remove find-fn list)]
    (vec (flatten
           (into []
             [(take new-location removed-list)
              item
              (drop new-location removed-list)])))))

(defn reorder-todo [list todo new-location]
  (let [new-todos (splice #(= % todo) (:todos list) new-location)]
    (assoc list :todos new-todos)))

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

(defrecord ReorderTodoCommand [list-id todo-id new-position]
  Command
  (apply-command [this lists]
    (let [list (get lists list-id)
          todo (first (filter #(= todo-id (:id %)) (:todos list)))]
      (.log js/console (str "Reordering " (:name todo) " in " (:name list)))
      (assoc lists list-id (reorder-todo list todo new-position)))))