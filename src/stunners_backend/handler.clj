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

(def schema [{:db/ident :user/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}])

(<!! (d/transact conn {:tx-data schema}))

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
