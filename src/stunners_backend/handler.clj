(ns stunners-backend.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [clojure.spec.alpha :as s]
            [stunners-backend.specs]
            [clj-http.client :as http]
            [stunners-backend.datomic :refer [conn]]
            [stunners-backend.utils :as utils]
            [stunners-backend.middleware :as middleware]
            [spec-tools.core :as st]
            [mount.core :as mount]))

(defroutes app-routes
  (GET "/health-check" [] {:status 200
                           :body {:status :ok}})
  (GET "/stylists" []
       (let [response (->> (d/q '[:find (pull ?s [:db/id :user/name :location/outcode :stylist/headline :stylist/images :stylist/description :user/avatar])
                                  (pull ?p [* {:product/type [:db/ident]}])
                                  (pull ?pt [:db/id :db/ident])
                                  :where [?s :stylist/headline]
                                  [?p :product/stylist ?s]
                                  [?p :product/type ?pt]]
                                (d/db conn))
                           utils/inline-enums
                           utils/split-main-related)]
         {:status 200
          :body response}))

  (GET "/user" {:keys [user/auth0-id]}
       (if-let [user (d/q '[:find (pull ?e [:db/id :user/name :user/email :user/phone-number :user/avatar :user/auth0-id :location/address]) .
                            :where [?e :user/auth0-id ?id]
                            :in $ ?id]
                          (d/db conn)
                          auth0-id)]
         {:status 200
          :body user}
         {:status 404
          :body {:message (str "User with auth0 id " auth0-id " not found")}}))

  (POST "/user" {:keys [params]}
        (let [user (st/select-spec :request/user params)]
          (if (s/valid? :request/user user)
            {:status 200
             :body (-> @(d/transact conn [user])
                       (select-keys [:tx-data]))}
            (utils/spec-failed-response :request/user user))))

  (GET "/appointments" {:keys [user/auth0-id params]}
       {:status 200
        :body (->> (d/q '[:find
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
        (let [{:keys [location/lat location/lng] :as appointment}
              (st/select-spec :request/appointment params)]
          (if (s/valid? :request/appointment appointment)
            (let [{address-components :address_components address :formatted_address}
                  (-> (str "https://maps.googleapis.com/maps/api/geocode/json?latlng=" lat "," lng)
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
                  stylee (d/q '[:find ?e .
                                :where [?e :user/auth0-id ?auth0-id]
                                :in $ ?auth0-id]
                              (d/db conn)
                              auth0-id)
                  transaction (merge appointment {:location/address address
                                                  :location/postcode postcode
                                                  :appointment/status :appointment-status/pending
                                                  :appointment/stylee stylee})]
              {:status 200
               :body (-> @(d/transact conn [transaction])
                         (select-keys [:tx-data]))})
            (utils/spec-failed-response :request/appointment appointment))))

  (PUT "/appointments/:id" {:keys [user/auth0-id] {param-id :id :as params} :params}
       (let [appointment-id (Long/parseLong param-id)
             appointment-update (st/select-spec :request/appointment-update params)
             db (d/db conn)
             current-user-id (d/q '[:find ?e .
                                    :where [?e :user/auth0-id ?auth0-id]
                                    :in $ ?auth0-id]
                                  db auth0-id)
             {{stylist-id :db/id} :appointment/stylist
              {status :db/ident} :appointment/status}
             (d/pull db '[:appointment/stylist {:appointment/status [:db/ident]}] appointment-id)]
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
                  :body (let [transactions (map (fn [[k v]]
                                                  [:db/add appointment-id k v])
                                                appointment-update)]
                          (-> @(d/transact conn transactions)
                              (select-keys [:tx-data])))})))

  (route/not-found {:status 404
                    :body {:message "route not found"}}))

(def app
  (-> app-routes
      middleware/handle-exceptions
      (middleware/authenticate {:client-secret (env :auth0-client-secret)
                                :client-id "Uc6xSnmrrj9L155vsIrpAhXrHnUMGX6w"
                                :domain "dwmartin41.eu.auth0.com"})
      middleware/edn
      wrap-edn-params
      (wrap-defaults api-defaults)))

(defn init []
  (mount/start))
