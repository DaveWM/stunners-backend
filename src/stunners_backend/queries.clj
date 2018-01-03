(ns stunners-backend.queries
  (:require [datomic.api :as d]
            [stunners-backend.utils :as utils]))


(defn get-auth0-user [db auth0-id]
  (d/q '[:find (pull ?e [:db/id :user/name :user/email :user/phone-number :user/avatar :user/auth0-id :location/address]) .
         :where [?e :user/auth0-id ?id]
         :in $ ?id]
       db
       auth0-id))

(defn get-appointments [db auth0-id]
  (->> (d/q '[:find
              (pull ?a [* {:appointment/status [:db/ident]} {:appointment/product-types [:db/ident]}])
              :where [?a :appointment/time]
              [?user :user/auth0-id ?auth0-id]
              (or [?a :appointment/stylist ?user]
                  [?a :appointment/stylee ?user])
              [?a :appointment/product-types ?pt]
              :in $ ?auth0-id]
            db
            auth0-id)
       utils/inline-enums
       utils/split-main-related))
