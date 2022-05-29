(ns front.events
  (:require [re-frame.core :as rf]
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

(defn notification-fx! [code message]
  (fn [_ _]
    (js/Notification. "CRUD Example" #js{:body message})
    {::create-code code}))

(js/Notification.requestPermission)

(rf/reg-event-fx :success-create (notification-fx! :success "Пациент успешно создан"))
(rf/reg-event-fx :error-on-create (notification-fx! :error "Ошибка при создании"))

;; ----------------LIST PAGE SECTION
(rf/reg-event-fx
 ::get-patients-list
 (fn [{db :db}]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/list"
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:success-list-get]
                 ;; :on-error        [:error-on-list-get]
                 }}))

(rf/reg-event-fx
 ::search-patients-list
 (fn [{db :db} [_ params]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             "/api/search"
                 :params          params
                 :format          (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:success-list-get]
                 ;; :on-error        [:error-on-list-get]
                 }}))

(rf/reg-event-fx
 :success-list-get
 (fn [{db :db} [_ response]]
   {:db (-> db
            (dissoc :loading?)
            (assoc  :patients-list (if (vector? response) response [response])))}))

(rf/reg-sub ::patients-list (fn [db _] (:patients-list db)))

(comment
  (require 're-frame.db)
  
  (rf/dispatch [:success-create nil])
  (rf/dispatch [:error-on-create nil])
  re-frame.db/app-db
  (rf/dispatch [:front.events/get-patients-list "Олег"])
  (rf/subscribe [:front.events/patients-list])

  ;;
  )
