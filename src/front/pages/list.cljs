(ns front.pages.list
  (:require [front.css-dsl :refer [style-tag]]
            [re-frame.core :as rf]
            front.events
            reagent.core
            goog.functions))

(rf/dispatch [:front.events/get-patients-list])

(def patients-list (rf/subscribe [:front.events/patients-list]))

(def ascending? (reagent.core/atom true))
(def sort-key (reagent.core/atom identity))

(defn sort-by* [key]
  (reset! sort-key key )
  (swap! ascending? not))

(def on-search
  (goog.functions/debounce
   (fn [ev]
     (let [value (-> ev .-target .-value)]
       (rf/dispatch [:front.events/search-patients-list {:search-value value}])))
   500))

(defn control-row []
  [:div#control-row
   [:input#search {:autoComplete :off
                   :on-change on-search}]
   [:button#filters "Фильтры"]
   [:button#create
    {:on-click #(aset js/location "pathname" "patients/create")}
    "Создать"]
   (style-tag
    {:#control-row
     {:display         :flex
      :justify-content :space-evenly
      :width           :100%
      :gap             :1vw
      :nested
      {:button  {:font-size      :1.2vw}
       :#search {:outline :none
                 :font-size      :1.2vw
                 :width   :70%}}}})])

(defn on-edit [id]
  #(aset js/location "pathname" (str "patients/edit/" id)))

(defn on-delete [id row-id]
  (fn [_]
    (when (js/confirm "Вы точно хотите удалить запись?")
      (rf/dispatch [:front.events/delete-patient id]))))

(defn view []
  [:div#main-container
   [control-row]
   [:div#list
    [:div.list-row
     [:h3 {:on-click #(sort-by* :name)} "Имя"]
     [:h3 {:on-click #(sort-by* :mname)} "Отчество"]
     [:h3 {:on-click #(sort-by* :lname)} "Фамилия"]
     [:h3 {:on-click #(sort-by* :sex)} "Пол"]
     [:h3 {:on-click #(sort-by* :bdate)} "Дата рождения"]
     [:h3 {:on-click #(sort-by* :address)} "Адрес"]
     [:h3 {:on-click #(sort-by* :oms_num)} "ОМС"]]
    (into
     [:<>]
     (for [{:keys [id name
                   mname lname
                   sex bdate address
                   oms_num]}
           (sort-by @sort-key (if @ascending? > <) @patients-list)
           :let [row-id (str "row-n-" id)]]
       [:div.list-row
        {:id row-id}
        [:div name]
        [:div mname]
        [:div lname]
        [:div sex]
        [:div bdate]
        [:div address]
        [:div oms_num]
        [:button.edit-btn
         {:on-click (on-edit id)}
         "edit"]
        [:button.delete-btn
         {:on-click (on-delete id row-id)}
         "delete"]]))
    (style-tag
     {:#main-container
      {:position  :fixed
       :top       :20%
       :left      :50%
       :transform {:translate [:-50% :-20%]}}
      :#list
      {:display        :flex
       :flex-direction :column
       :margin-top     :1vw
       :font-size      :1.2vw}
      :.list-row
      {:display :flex
       :gap     :1vw
       :margin-bottom :1vw
       :nested
       {:h3 {:user-select   :none
             :margin        0
             :padding       0}
        [:div :h3]
        {:width      :10vw
         :text-align :center}}}
      })]])
