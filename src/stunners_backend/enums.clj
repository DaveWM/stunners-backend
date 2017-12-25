(ns stunners-backend.enums)

(def product-type
  (->> [:haircut :waxing :nails]
       (map #(keyword "product-type" (name %)))
       (into #{})))

(def appointment-status
  #{:appointment-status/pending :appointment-status/confirmed :appointment-status/rejected})
