(ns front.pages.edit
  (:require [front.pages.create]
            [re-frame.core :as rf]))

(defn view [{id :id}]
  (rf/dispatch [:front.events/get-patient-info
                {:id id}
                front.pages.create/form-data])
  #(front.pages.create/view id))
