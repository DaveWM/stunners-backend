(ns stunners-backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [datomic.client :as d]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [jerks-whistling-tunes.core :as jwt]
            [jerks-whistling-tunes.sign :as sign]
            [stunners-backend.credentials :refer [credentials]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [clojure.spec.alpha :as s]
            [stunners-backend.specs]))

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
                          :db/cardinality :db.cardinality/one}])

(def schema (concat user-schema
                    location-schema
                    product-type-schema
                    product-schema
                    appointment-schema
                    stylist-schema
                    filter-group-schema))


(<!! (d/transact conn {:tx-data schema}))

(defn inline-enums [query-result]
  (clojure.walk/prewalk (fn [x]
                          (if (and (map? x) (= (count x) 1))
                            (if-let [ident (:db/ident x)]
                              ident
                              x)
                            x))
                        query-result))

(defn split-main-related [query-result]
  (let [[main & related] (->> query-result
                              (apply map vector)
                              (map distinct))]
    {:main main
     :related (flatten related)}))

(defn q
  ([query] (q query (d/db conn)))
  ([query db & inputs]
   (<!! (d/q conn {:query query :args (cons db inputs)}))))

(defn transact [txs]
  (<!! (d/transact conn {:tx-data txs})))


(defn authenticate [handler credentials]
  "Authenticates using auth token in Authorization header, or query param if header not set. Adds the auth token onto the request as :token, and the user's auth0 id as :user/auth0-id"
  (fn [request]
    (let [{:keys [headers query-params]} request
          token (-> (get headers "authorization")
                    (or "")
                    (str/replace #"^\s*Bearer\s+" ""))
          decoded-token (jwt/validate token
                                      (jwt/signature (sign/hs256 (:client-secret credentials)))
                                      (jwt/aud (:client-id credentials))
                                      (jwt/iss (:domain credentials))
                                      jwt/exp)]
      (if-let [{:keys [sub]} decoded-token]
        (handler (-> request
                     (assoc :token token)
                     (assoc :user/auth0-id sub)))
        {:status 401
         :headers {"Content-Type" "application/edn"}
         :body (pr-str {:message "Auth token not found or is invalid"})}))))

(defroutes app-routes
  (GET "/health-check" [] {:status 200
                           :headers {"Content-Type" "application/edn"}
                           :body (pr-str {:status :ok})})
  (GET "/stylists" []
       (->> (q '[:find (pull ?s [:db/id :user/name :location/outcode :stylist/headline :stylist/images :stylist/description :user/avatar])
                 (pull ?p [* {:product/type [:db/ident]}])
                 (pull ?pt [:db/id :db/ident])
                 :where [?s :stylist/headline]
                 [?p :product/stylist ?s]
                 [?p :product/type ?pt]])
            inline-enums
            split-main-related
            pr-str))

  (GET "/user" {:keys [user/auth0-id]}
       (if-let [user (->> (q '[:find (pull ?e [:db/id :user/name :user/email :user/phone-number :user/avatar :user/auth0-id :location/address])
                               :where [?e :user/auth0-id ?id]
                               :in $ ?id]
                             (d/db conn)
                             auth0-id)
                          ffirst)]
         (pr-str user)
         {:status 404
          :headers {"Content-Type" "application/edn"}
          :body (pr-str {:message (str "User with auth0 id " auth0-id " not found")})}))

  (POST "/user" {:keys [params]}
        (let [user (select-keys params [:user/name :user/email :user/avatar :user/phone-number :user/auth0-id :location/address :user/auth0-id])]
          (if (s/valid? :request/user user)
            {:status 200
             :headers {"Content-Type" "application/edn"}
             :body (-> (transact [user])
                       (select-keys [:tx-data])
                       pr-str)}
            {:status 400
             :headers {"Content-Type" "application/edn"}
             :body (pr-str {:message "Invalid request"
                            :explanation (s/explain-data :request/user user)})})))

  (GET "/appointments" {:keys [user/auth0-id params]}
       {:status 200
        :headers {"Content-Type" "application/edn"}
        :body (->> (q '[:find
                        (pull ?a [* {:appointment/status [:db/ident]}])
                        (pull ?pt [:db/id :db/ident])
                        :where [?a :appointment/time]
                        [?user :user/auth0-id ?auth0-id]
                        (or [?a :appointment/stylist ?user]
                            [?a :appointment/stylee ?user])
                        [?a :appointment/product-types ?pt]
                        :in $ ?auth0-id]
                      (d/db conn)
                      auth0-id)
                   inline-enums
                   split-main-related
                   pr-str)})

  (POST "/appointments" {:keys [user/auth0-id params]}
        )

  (route/not-found {:status 404
                    :body (pr-str {:message "route not found"})}))

(def app
  (-> app-routes
      (authenticate credentials)
      wrap-edn-params
      (wrap-defaults api-defaults)))
