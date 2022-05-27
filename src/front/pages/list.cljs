(ns front.pages.list
  (:require [front.css-dsl :refer [style-tag]]
            reagent.core))

(def data-set)

(def data-set-copy (reagent.core/atom data-set))
(def ascending? (reagent.core/atom true))

(defn sort-by* [key]
  (swap! data-set-copy (partial sort-by key (if @ascending? > <)))
  (swap! ascending? not))

(defn control-row []
  [:div#control-row
   [:input#search {:autocomplete :off}]
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

(defn view []
  [:div#main-container
   [control-row]
   [:div#list
    [:div.list-row
     [:h3 {:on-click #(sort-by* :name)} "Имя"]
     [:h3 {:on-click #(sort-by* :mname)} "Имя"]
     [:h3 {:on-click #(sort-by* :lname)} "Имя"]
     [:h3 {:on-click #(sort-by* :sex)} "Пол"]
     [:h3 {:on-click #(sort-by* :bdate)} "Дата рождения"]
     [:h3 {:on-click #(sort-by* :address)} "Адрес"]
     [:h3 {:on-click #(sort-by* :oms_num)} "ОМС"]]
    (into
     [:<>]
     (for [{:keys [name mname lname
                   sex bdate address
                   oms_num]} @data-set-copy]
       [:div.list-row
        [:div name]
        [:div mname]
        [:div lname]
        [:div sex]
        [:div bdate]
        [:div address]
        [:div oms_num]]))
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
       :nested
       {:h3 {:user-select   :none
             :margin        0
             :margin-bottom :1vw
             :padding       0}
        [:div :h3]
        {:width      :10vw
         :text-align :center}}}})]])
