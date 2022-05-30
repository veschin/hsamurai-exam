(ns front.pages.create
  (:require [front.css-dsl :refer [style-tag]]
            [clojure.spec.alpha :as spec]
            [front.spec.spec-create :refer [valid?]]
            [clojure.string :refer [join]]
            reagent.core
            [re-frame.core :as rf]
            front.events))

(def form-data (reagent.core/atom {:data/mname ""}))
(def form-data-valid? (reagent.core/atom nil))
(def wrong-fields (reagent.core/atom #{}))

(defn on-check [value]
  (fn [_]
    (swap! form-data assoc :data/sex value)
    (reset! form-data-valid? (valid? @form-data))))

(defn assoc-data! [key]
  (fn [ev]
    (let [val (-> ev .-target .-value)]
      (swap! wrong-fields
             (if (spec/valid? key val)
               disj
               conj)
             key)
      (if (and (= val "") (not (#{:data/mname} key)))
        (swap! form-data dissoc key)
        (swap! form-data assoc key val))
      (reset! form-data-valid? (valid? @form-data)))))

(defn value-or-empty-str [key]
  (get @form-data key ""))

(rf/reg-fx :front.events/create-code (fn [code] (when (#{:success} code) (reset! form-data {:data/mname ""}))))

(def address-ks [:data/country :data/city :data/street
                 :data/house :data/apartment])

(defn- ->address [data]
  (->> (select-keys data address-ks)
       vals
       (join ", ")))

(defn- prepare-to-post [data]
  (->> (assoc data :address (->address data))
       (#(apply dissoc % address-ks))
       (map (fn [[key val]] {(name key) val}))
       (reduce into {})))

(defn on-create [_] (rf/dispatch [:create-patient (prepare-to-post @form-data)]))

(defn on-edit [edit-id]
  (fn [_]
    (let [data (assoc @form-data :id edit-id)]
      (rf/dispatch
       [:front.events/edit-patient-info
        (prepare-to-post data)
        form-data]))))

(defn view [& [edit-id]]
  [:div#main-container
   (into
    [:<>]
    (for [[key label]
          (partition-all 2 [:data/name      "Имя"
                            :data/mname     "Отчество (при наличии)"
                            :data/lname     "Фамилия"
                            :data/bdate     "Дата рождения"
                            :data/country   "Страна"
                            :data/city      "Город"
                            :data/street    "Улица"
                            :data/house     "Дом"
                            :data/apartment "Квартира"
                            :data/oms_num   "Полис ОМС"])]
      [:div.input-row
       [:label label]
       [:input {:className (when (@wrong-fields key) :error)
                :value     (value-or-empty-str key)
                :on-change (assoc-data! key)}]]))
   [:div.input-row
    [:label "Пол"]
    [:div.checkbox-row
     [:input {:type     :checkbox
              :checked  (boolean (#{"Male"} (:data/sex @form-data)))
              :on-change (on-check "Male")}]
     [:label "Мужчина"]]
    [:div.checkbox-row
     [:input {:type     :checkbox
              :checked  (boolean (#{"Female"} (:data/sex @form-data)))
              :on-change (on-check "Female")}]
     [:label "Женщина"]]]
   [:div#button-row
    (if edit-id
      [:button
       {:disabled (not @form-data-valid?)
        :on-click (on-edit edit-id)}
       "Изменить"]
      [:button
       {:disabled (not @form-data-valid?)
        :on-click on-create}
       "Создать"])]
   (style-tag
    {:#main-container
     {:position  :fixed
      :top       :40%
      :left      :50%
      :transform {:translate [:-50% :-40%]}
      :font-size :1.2vw}
     :.input-row
     {:display       :flex
      :gap           :1vw
      :margin-bottom :1vw
      :nested        {:label {:width :60%}
                      :input {:outline :none
                              :font-size :1.2vw}}}
     :.checkbox-row {:display         :flex
                     :align-items     :center
                     :justify-content :space-around}
     :#button-row   {:display         :flex
                     :justify-content :center
                     :nested          {:button {:font-size :1.2vw}}}
     :.error        {:background-color :red}})])


(comment
  (->> {:data/name      "Олег"
        ;; :data/mname     "Олегович"
        :data/lname     "Вещин"
        :data/bdate     "19.10.2000"
        :data/country   "Россия"
        :data/city      "Барнаул"
        :data/street    "Малахова"
        :data/house     "100"
        :data/apartment "170"
        :data/oms_num   "1234567890000000"
        :data/sex       "Male"}
       (swap! form-data merge)
       (valid?))

  (on-create nil)
  
  
  ;;
  )
