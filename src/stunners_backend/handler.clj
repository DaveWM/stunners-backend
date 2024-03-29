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
            [stunners-backend.queries :as queries]
            [stunners-backend.email :as email]
            [stunners-backend.config :refer [config]]
            [spec-tools.core :as st]
            [mount.core :as mount :refer [defstate]])
  (:import (java.text SimpleDateFormat)))

(defroutes app-routes
           (GET "/health-check" [] {:status 200
                                    :body   {:status :ok}})
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
                :body   response}))

           (GET "/user" {:keys [user/auth0-id]}
             (if-let [user (queries/get-auth0-user (d/db conn) auth0-id)]
               {:status 200
                :body   user}
               {:status 404
                :body   {:message (str "User with auth0 id " auth0-id " not found")}}))

           (POST "/user" {:keys [params :user/auth0-id]}
             (let [user (st/select-spec :request/user params)]
               (if (s/valid? :request/user user)
                 {:status 200
                  :body   (-> @(d/transact conn [(assoc user :user/auth0-id auth0-id)])
                              :db-after
                              (queries/get-auth0-user auth0-id))}
                 (utils/spec-failed-response :request/user params))))

           (GET "/appointments" {:keys [user/auth0-id params]}
             {:status 200
              :body   (queries/get-appointments (d/db conn) auth0-id)})

           (POST "/appointments" {:keys [user/auth0-id coeffects/current-time params]}
             (let [{:keys [location/lat location/lng] :as appointment}
                   (st/select-spec :request/appointment (assoc params :coeffects/current-time current-time))]
               (if (s/valid? :request/appointment appointment)
                 (let [{address-components :address_components address :formatted_address}
                       (-> (str "https://maps.googleapis.com/maps/api/geocode/json?latlng=" lat "," lng "&key=" (:google-maps-key config))
                           (http/get {:as :json})
                           :body
                           :results
                           first)
                       db (d/db conn)
                       with-type (fn [address-component type]
                                   (some #(= type %) (:types address-component)))
                       postcode (->> address-components
                                     (filter #(with-type % "postal_code"))
                                     first
                                     :long_name)
                       [stylee-id stylee-name] (d/q '[:find [?e ?name]
                                                      :where
                                                      [?e :user/auth0-id ?auth0-id]
                                                      [?e :user/name ?name]
                                                      :in $ ?auth0-id]
                                                    db
                                                    auth0-id)
                       stylist (d/entity db (:appointment/stylist appointment))
                       tx-data (-> appointment
                                   (dissoc :coeffects/current-time)
                                   (merge {:location/address   address
                                           :location/postcode  postcode
                                           :appointment/status :appointment-status/pending
                                           :appointment/stylee stylee-id}))
                       transaction @(d/transact conn [tx-data])]

                   (email/send! (:user/email stylist)
                                "Stunners: you've received an appointment request!"
                                (str "You've received an appointment request from " stylee-name ", "
                                     "for " (.format (SimpleDateFormat. "dd/MM/yyyy HH:mm") (:appointment/time appointment)) ". "
                                     "To accept or reject this request, please go to \"My Appointments\" in the Stunners app."))

                   {:status 200
                    :body   (-> transaction
                                :db-after
                                (queries/get-appointments auth0-id))})
                 (utils/spec-failed-response :request/appointment (assoc params :coeffects/current-time current-time)))))

           (PUT "/appointments/:id" {:keys [user/auth0-id] {param-id :id :as params} :params}
             (let [appointment-id (Long/parseLong param-id)
                   appointment-update (st/select-spec :request/appointment-update params)
                   db (d/db conn)
                   current-user-id (d/q '[:find ?e .
                                          :where [?e :user/auth0-id ?auth0-id]
                                          :in $ ?auth0-id]
                                        db auth0-id)
                   {{stylist-id :db/id} :appointment/stylist
                    status              :appointment/status
                    :as                 appointment}
                   (d/entity db appointment-id)]
               (cond
                 (not (s/valid? :request/appointment-update appointment-update))
                 (utils/spec-failed-response :request/appointment-update params)

                 (nil? stylist-id) {:status 404
                                    :body   {:message "Specified appointment does not exist"}}

                 (not= stylist-id current-user-id) {:status 400
                                                    :body   {:message "You cannot update an appointment where you are not the stylist"}}

                 (not= status :appointment-status/pending) {:status 200
                                                            :body   {:message "The appointment must be in the 'pending' state to update it"}}

                 :else {:status 200
                        :body   (let [tx-data (map (fn [[k v]]
                                                     [:db/add appointment-id k v])
                                                   appointment-update)
                                      transaction @(d/transact conn tx-data)]

                                  (email/send! (get-in appointment [:appointment/stylee :user/email])
                                               (str "Stunners: your appointment has been " (name (:appointment/status appointment-update)))
                                               (str "Your appointment with " (get-in appointment [:appointment/stylist :user/name])
                                                    " for " (.format (SimpleDateFormat. "dd/MM/yyyy HH:mm") (:appointment/time appointment)) " "
                                                    "has been " (name (:appointment/status appointment-update)) ". "
                                                    "You can view your appointment in the Stunners app."))

                                  {:main (-> transaction
                                             :db-after
                                             (d/pull [:db/id {:appointment/status [:db/ident]}] appointment-id)
                                             utils/inline-enums
                                             vector)})})))

           (route/not-found {:status 404
                             :body   {:message "route not found"}}))

(defstate app
  :start (-> app-routes
             middleware/handle-exceptions
             (middleware/authenticate (:auth0 config))
             middleware/edn
             middleware/wrap-cors
             wrap-edn-params
             middleware/add-current-time
             (wrap-defaults api-defaults)))

(defn init []
  (mount/start))
