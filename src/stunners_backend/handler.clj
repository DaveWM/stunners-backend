(ns stunners-backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [datomic.client :as d]
            [clojure.core.async :refer [<!!]]))

(def conn (<!! (d/connect
                {:db-name "hello"
                 :account-id d/PRO_ACCOUNT
                 :secret "mysecret"
                 :region "none"
                 :endpoint "localhost:8998"
                 :service "peer-server"
                 :access-key "myaccesskey"})))

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

(def filter-group-schema [{:db/ident :filter-group/label
                           :db/valueType :db.type/string
                           :db/cardinality :db.cardinality/one}
                          {:db/ident :filter-group/product-types
                           :db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/many}])

(def product-type-schema (->> [:haircut :waxing :nails]
                              (map #(keyword "product-type" (name %)))
                              (map #(hash-map :db/ident %))))

(def product-schema [{:db/ident :product/type
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :product/cost
                      :db/valueType :db.type/double
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :product/stylist
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}])

(def appointment-schema [{:db/ident :appointment/time
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
                         {:db/ident :appointment-status/pending}
                         {:db/ident :appointment-status/confirmed}
                         {:db/ident :appointment-status/rejected}
                         {:db/ident :appointment/status
                          :db/valueType :db.type/ref
                          :db/cardinality :db.cardinality/many}])

(def schema (concat user-schema
                    location-schema
                    product-type-schema
                    product-schema
                    appointment-schema
                    stylist-schema
                    filter-group-schema))


;(<!! (d/transact conn {:tx-data schema}))

(defroutes app-routes
  (GET "/health-check" [] {:status 200
                           :headers {"Content-Type" "application/edn"}
                           :body (pr-str {:status :ok})})
  (GET "/users" []
       (let [db (d/db conn)
             query '[:find ?name :where [_ :user/name ?name]]]
         (println query)
         {:status 200
          :headers {"Content-Type" "application/edn"}
          :body (->> (d/q conn {:query query :args [db]})
                     <!!
                     pr-str)}))
  (route/not-found {:status 404}))

(def app
  (wrap-defaults app-routes api-defaults))
