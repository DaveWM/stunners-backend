(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [mount.core :refer [defstate]]
            [stunners-backend.schema :refer [schema]]))


(def db-uri "datomic:ddb://eu-west-1/stunners-storage/main" #_"datomic:dev://localhost:4334/stunners")

(defstate conn
  :start (do (d/create-database db-uri)
             (let [connection (d/connect db-uri)]
               @(d/transact connection schema)
               connection))
  :stop (.release conn))
