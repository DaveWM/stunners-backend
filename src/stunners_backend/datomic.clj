(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [mount.core :refer [defstate]]
            [stunners-backend.schema :refer [schema]]
            [stunners-backend.config :refer [config]]))

(defstate conn
  :start (let [db-uri (get-in config [:datomic :db-uri])]
           (d/create-database db-uri)
             (let [connection (d/connect db-uri)]
               @(d/transact connection schema)
               connection))
  :stop (.release conn))
