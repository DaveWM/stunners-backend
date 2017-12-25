(ns stunners-backend.datomic
  (:require [datomic.client :as d]
            [clojure.core.async :refer [<!!]]))


(def conn (<!! (d/connect
                {:db-name "hello"
                 :account-id d/PRO_ACCOUNT
                 :secret "mysecret"
                 :region "none"
                 :endpoint "localhost:8998"
                 :service "peer-server"
                 :access-key "myaccesskey"})))

(defn q
  ([query] (q query (d/db conn)))
  ([query db & inputs]
   (<!! (d/q conn {:query query :args (cons db inputs)}))))

(defn transact [txs]
  (<!! (d/transact conn {:tx-data txs})))
