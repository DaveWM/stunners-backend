(ns stunners-backend.queries
  (:require [datomic.api :as d]))


(defn get-auth0-user [db auth0-id]
  (d/q '[:find (pull ?e [:db/id :user/name :user/email :user/phone-number :user/avatar :user/auth0-id :location/address]) .
         :where [?e :user/auth0-id ?id]
         :in $ ?id]
       db
       auth0-id))
