(ns front.spec.spec-create
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [clojure.test :as t]))

(spec/def ::letter (partial re-matches #"[a-zA-Z|а-яА-Я]"))
(spec/def ::word (spec/coll-of ::letter))
(spec/def ::text-field
  #(->> (seq %)
        (filter (comp not #{" " "-"}))
        (spec/valid? ::word)))

(spec/def ::optional-text-field
  (or (partial spec/valid? ::text-field) #{""}))

(spec/def ::int-digit (partial re-matches #"\d+"))
(spec/def ::date
  (spec/and
   (partial re-matches #"\d{2}\.\d{2}\.\d{4}")
   (fn [date-str]
     (let [cy         #?(:cljs (.getFullYear (js/Date.))
                         :clj  (.format (java.text.SimpleDateFormat. "yyyy")
                                       (java.util.Date.)))
           [d m y]    (string/split date-str #"\.")
           ->int      #? (:cljs #(js/parseInt %)
                          :clj  #(Integer/parseInt %))
           less-or-eq (fn [d cd] (<= (->int d) (->int cd)))]
       (->> [#_(less-or-eq y (- (->int cy) 14))
             (less-or-eq m 12)
             (less-or-eq 1 m)
             (if (#{"02"} m) (less-or-eq d 28) nil)
             (cond
               (and (#{"01" "03" "05"
                       "07" "08" "10" "12"} m)
                    (less-or-eq d 31))
               true
               
               (less-or-eq d 30)
               true

               :default
               false)]
            (filter some?)
            (every? true?))))))
(spec/def ::oms (spec/and (comp #{16} count) ::int-digit))

(t/deftest valid-string-field?
  (t/is (spec/valid? ::text-field "John"))
  (t/is (spec/valid? ::text-field "seVen"))
  (t/is (spec/valid? ::text-field "doE"))
  (t/is (spec/valid? ::text-field "Los-Angeles"))
  (t/is (not (spec/valid? ::text-field "bang!"))))

(t/deftest valid-int-numbers?
  (t/is (spec/valid? ::int-digit "7"))
  (t/is (spec/valid? ::int-digit "777"))
  (t/is (not (spec/valid? ::int-digit "seveN"))))

(t/deftest valid-date?
  (t/is (spec/valid? ::date "15.09.1995"))
  (t/is (spec/valid? ::date "19.10.2000"))
  (t/is (spec/valid? ::date "28.02.2005"))
  (t/is (not (spec/valid? ::date "29.02.2000")))
  (t/is (not (spec/valid? ::date "11/22/44")))
  (t/is (not (spec/valid? ::date "11.22.44")))
  (t/is (not (spec/valid? ::date "11.12.2009"))))

(t/deftest valid-oms?
  (t/is (spec/valid? ::oms "1234567890000000"))
  (t/is (spec/valid? ::oms "7777777777777777"))
  (t/is (not (spec/valid? ::oms "777777777777777")))
  (t/is (not (spec/valid? ::oms "77777777777777778"))))

(spec/def :data/name ::text-field)
(spec/def :data/mname ::optional-text-field)
(spec/def :data/lname ::text-field)
(spec/def :data/bdate ::date)
(spec/def :data/country ::text-field)
(spec/def :data/city ::text-field)
(spec/def :data/street ::text-field)
(spec/def :data/house ::int-digit)
(spec/def :data/apartment ::int-digit)
(spec/def :data/oms_num ::oms)
(spec/def :data/sex ::text-field)
(spec/def :data/form
  (spec/keys
   :req [:data/name :data/mname
            :data/lname :data/bdate
            :data/country :data/city
            :data/house :data/street
            :data/oms_num :data/apartment :data/sex]))

(defn valid? [hash-map]
  (spec/valid? :data/form hash-map))

(comment
  (do (valid-date?)
      (valid-int-numbers?)
      (valid-oms?)
      (valid-string-field?))

  (spec/valid?
   :data/form
   {:data/name      "Олег"
    :data/mname     ""
    :data/lname     "Вещин"
    :data/bdate     "19.10.2000"
    :data/country   "Россия"
    :data/city      "Барнаул"
    :data/street    "Малахова"
    :data/house     "100"
    :data/apartment "170"
    :data/oms_num   "1234567890000000"})

  (spec/valid? :data/bdate "19.12.2007")

  (map string/trim (string/split "ул. Пухова, дом Скоропутова" #","))
  (spec/valid? ::text-field "Алоа Вера")

  ;; TODO PREPROCESS
  (string/lower-case "111")
  (string/trim "12321")
  
  ;;
  )
