(ns stunners-backend.schema
  (:require [stunners-backend.enums :as enums]))

(def user-schema [{:db/ident :user/name
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/avatar
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/email
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/phone-number
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :user/auth0-id
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}])

(def stylist-schema [{:db/ident :stylist/headline
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :stylist/images
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/many}
                     {:db/ident :stylist/description
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}])

(def location-schema [{:db/ident :location/address
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :location/outcode
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :location/postcode
                       :db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :location/lat
                       :db/valueType :db.type/double
                       :db/cardinality :db.cardinality/one}
                      {:db/ident :location/lng
                       :db/valueType :db.type/double
                       :db/cardinality :db.cardinality/one}])

(def product-type-schema (map #(hash-map :db/ident %) enums/product-type))

(def product-schema [{:db/ident :product/type
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :product/cost
                      :db/valueType :db.type/double
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :product/stylist
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}])

(def appointment-schema (concat [{:db/ident :appointment/time
                                  :db/valueType :db.type/instant
                                  :db/cardinality :db.cardinality/one}
                                 {:db/ident :appointment/stylist
                                  :db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/one}
                                 {:db/ident :appointment/stylee
                                  :db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/one}
                                 {:db/ident :appointment/product-types
                                  :db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/many}
                                 {:db/ident :appointment/status
                                  :db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/one}]
                                (map #(hash-map :db/ident %) enums/appointment-status)))

(def schema (concat user-schema
                    location-schema
                    product-type-schema
                    product-schema
                    appointment-schema
                    stylist-schema))
