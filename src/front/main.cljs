(ns front.main
  (:require
   [reagent.dom]
   [front.pages.dispatcher :refer [match-route]]
   ))


(defn mount []
  (reagent.dom/render
   [(match-route js/location.pathname)]
   (js/document.querySelector "#app")))

(mount)

