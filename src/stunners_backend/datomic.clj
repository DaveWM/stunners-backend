(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [mount.core :refer [defstate]]
            [stunners-backend.schema :refer [schema]]))


(def db-uri "datomic:dev://localhost:4334/hello")

(defstate conn
  :start (let [connection (d/connect db-uri)]
           @(d/transact connection schema)
           connection)
  :stop (.release conn))
