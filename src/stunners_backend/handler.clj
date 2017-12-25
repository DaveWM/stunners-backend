(ns stunners-backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [datomic.client :as d]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [stunners-backend.credentials :refer [credentials]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [clojure.spec.alpha :as s]
            [stunners-backend.specs]
            [clj-http.client :as http]
            [stunners-backend.datomic :refer [conn q transact]]
            [stunners-backend.schema :refer [schema]]
            [stunners-backend.utils :as utils]
            [stunners-backend.middleware :as middleware]
            [spec-tools.core :as st]))


(<!! (d/transact conn {:tx-data schema}))

(defroutes app-routes
  (GET "/health-check" [] {:status 200
                           :body {:status :ok}})
  (GET "/stylists" []
       (let [response (->> (q '[:find (pull ?s [:db/id :user/name :location/outcode :stylist/headline :stylist/images :stylist/description :user/avatar])
                                (pull ?p [* {:product/type [:db/ident]}])
                                (pull ?pt [:db/id :db/ident])
                                :where [?s :stylist/headline]
                                [?p :product/stylist ?s]
                                [?p :product/type ?pt]])
                           utils/inline-enums
                           utils/split-main-related)]
         {:status 200
          :body response}))

  (GET "/user" {:keys [user/auth0-id]}
       (if-let [user (->> (q '[:find (pull ?e [:db/id :user/name :user/email :user/phone-number :user/avatar :user/auth0-id :location/address])
                               :where [?e :user/auth0-id ?id]
                               :in $ ?id]
                             (d/db conn)
                             auth0-id)
                          ffirst)]
         {:status 200
          :body user}
         {:status 404
          :body {:message (str "User with auth0 id " auth0-id " not found")}}))

  (POST "/user" {:keys [params]}
        (let [user (st/select-spec :request/user params)]
          (if (s/valid? :request/user user)
            {:status 200
             :body (-> (transact [user])
                       (select-keys [:tx-data]))}
            (utils/spec-failed-response :request/user user))))

  (GET "/appointments" {:keys [user/auth0-id params]}
       {:status 200
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
                   utils/inline-enums
                   utils/split-main-related)})

  (POST "/appointments" {:keys [user/auth0-id params]}
        (let [appointment (st/select-spec :request/appointment params)]
          (if (s/valid? :request/appointment appointment)
            (let [{address-components :address_components address :formatted_address}
                  (-> (str "https://maps.googleapis.com/maps/api/geocode/json?latlng=" 51.4297356 "," -0.163282)
                      (http/get {:as :json})
                      :body
                      :results
                      first)
                  with-type (fn [address-component type]
                              (some #(= type %) (:types address-component)))
                  postcode (->> address-components
                                (filter #(with-type % "postal_code"))
                                first
                                :long_name)
                  stylee (-> (q '[:find ?e
                                  :where [?e :user/auth0-id ?auth0-id]
                                  :in $ ?auth0-id]
                                (d/db conn)
                                auth0-id)
                             ffirst)]
              {:status 200
               :body (-> (merge appointment {:location/address address
                                             :location/postcode postcode
                                             :appointment/status :appointment-status/pending
                                             :appointment/stylee stylee})
                         vector
                         transact
                         (select-keys [:tx-data]))})
            (utils/spec-failed-response :request/appointment appointment))))

  (PUT "/appointments/:id" {:keys [user/auth0-id] {param-id :id :as params} :params}
       (let [appointment-id (Long/parseLong param-id)
             appointment-update (st/select-spec :request/appointment-update params)
             db (d/db conn)
             current-user-id (-> (q '[:find ?e
                                      :where [?e :user/auth0-id ?auth0-id]
                                      :in $ ?auth0-id]
                                    db auth0-id)
                                 ffirst)
             {{stylist-id :db/id} :appointment/stylist
              {status :db/ident} :appointment/status}
             (<!! (d/pull db {:selector '[:appointment/stylist {:appointment/status [:db/ident]}]
                              :eid appointment-id}))]
         (cond
           (not (s/valid? :request/appointment-update appointment-update))
           (utils/spec-failed-response :request/appointment-update appointment-update)

           (nil? stylist-id) {:status 404
                              :body {:message "Specified appointment does not exist"}}

           (not= stylist-id current-user-id) {:status 400
                                              :body {:message "You cannot update an appointment where you are not the stylist"}}

           (not= status :appointment-status/pending) {:status 200
                                                      :body {:message "The appointment must be in the 'pending' state to update it"}}

           :else {:status 200
                  :body (-> (map (fn [[k v]]
                                   [:db/add appointment-id k v])
                                 appointment-update)
                            transact
                            (select-keys [:tx-data]))})))

  (route/not-found {:status 404
                    :body {:message "route not found"}}))

(def app
  (-> app-routes
      middleware/edn
      (middleware/authenticate credentials)
      wrap-edn-params
      (wrap-defaults api-defaults)))
