(ns front.events
  (:require [re-frame.core :as rf]
            [clojure.string :as string]
            day8.re-frame.http-fx
            [ajax.core :as ajax]))

;; ----------------CREATE PAGE SECTION
(rf/reg-event-fx
 :create-patient
 (fn [_ [_ params]]
   {:http-xhrio {:method          :post
                 :uri             "/api/create"
                 :params          params
                 ;; :timeout         8000      
                 :format          (ajax/json-request-format)
                 :response-format (ajax/ring-response-format)
                 :on-success      [:success-create]
                 :on-failure      [:error-on-create]}}))

(defn notification-fx!
  ([message] (js/Notification. "CRUD Example" #js{:body message}))
  ([code message]
   (fn [_ _]
     (js/Notification. "CRUD Example" #js{:body message})
     {::create-code code})))

(js/Notification.requestPermission)

(rf/reg-event-fx :success-create (notification-fx! :success "Пациент успешно создан"))
(rf/reg-event-fx :error-on-create (notification-fx! :error "Ошибка при создании"))

;; ----------------LIST PAGE SECTION
(rf/reg-event-fx
 ::get-patients-list
 (fn [{db :db}]
   {:db         (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/list"
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:success-list-get]
                 :on-failure      [:error-on-list-get]}}))

(rf/reg-event-fx
 ::search-patients-list
 (fn [{db :db} [_ params]]
   {:db         (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/search"
                 :params          params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:success-list-get]
                 :on-failure      [:error-on-list-get]}}))

(rf/reg-event-fx
 :success-list-get
 (fn [{db :db} [_ response]]
   {:db (-> db
            (dissoc :loading?)
            (assoc  :patients-list (if (vector? response) response [response])))}))

(rf/reg-event-fx
 :error-on-list-get
 (fn [{db :db} _]
   {:db (-> db (dissoc :loading?) (assoc :patients-list []))}))

(rf/reg-sub ::patients-list (fn [db _] (:patients-list db)))

(rf/reg-event-fx
 ::delete-patient
 (fn [_ [_ params]]
   {:http-xhrio {:method          :post
                 :uri             "/api/delete"
                 :params          params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/ring-response-format)
                 :on-success      [:success-patient-delete]
                 :on-failure      [:error-on-action]}}))

(rf/reg-event-fx
 :success-patient-delete
 (fn [_ _] (notification-fx! "Пациент успешно удален")))

;; ----------------EDIT PAGE SECTION
(rf/reg-event-fx
 ::edit-patient-info
 (fn [{db :db} [_ params form-data-atom]]
   {:db         (assoc db :form-data-atom form-data-atom)
    :http-xhrio {:method          :post
                 :uri             "/api/edit"
                 :params          params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/ring-response-format)
                 :on-success      [:success-patient-edit]
                 :on-failure      [:error-on-action]}}))

(rf/reg-event-fx
 ::get-patient-info
 (fn [{db :db} [_ params form-data-atom]]
   {:db         (assoc db :form-data-atom form-data-atom)
    :http-xhrio {:method          :get
                 :uri             "/api/edit"
                 :params          params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:success-patient-info-get]
                 :on-failure      [:error-on-action]}}))

(defn update-form-data! [db response]
  (let [atom          (:form-data-atom db)
        split-address #(->> (string/split (:address %) ",")
                            (map string/trim)
                            (zipmap [:data/country :data/city :data/street
                                     :data/house :data/apartment])
                            (merge (dissoc % :address)))
        form-data     (->> response
                           split-address
                           (#(dissoc % :id))
                           (map (fn [[key val]] {(keyword "data" key) val}))
                           (reduce into {}))]
    (reset! atom form-data)
    nil))

(rf/reg-event-fx
 :success-patient-info-get
 (fn [{db :db} [_ response]]
   (update-form-data! db response)))

(rf/reg-event-fx
 :success-patient-edit
 (fn [_ _ ]
   (notification-fx! "Данные пациента успешно изменены")
   nil))

(rf/reg-event-fx :error-on-action (notification-fx! :error "Ошибка при получении данных с сервера"))

(comment
  (require 're-frame.db)
  
  (rf/dispatch [:success-create nil])
  (rf/dispatch [:error-on-create nil])
  (rf/dispatch [:error-on-list-get nil])
  re-frame.db/app-db
  (rf/dispatch [:front.events/get-patients-list "Олег"])
  (rf/subscribe [:front.events/patients-list])
  (def atom- (atom nil))
  (rf/dispatch [:front.events/get-patient-info {:id 1} atom-])
  

  ;;
  )
