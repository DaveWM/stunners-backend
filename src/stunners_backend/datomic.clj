(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [mount.core :refer [defstate]]
            [stunners-backend.schema :refer [schema]]))


(def db-uri "datomic:sql://stunners?jdbc:postgresql://ec2-54-217-217-194.eu-west-1.compute.amazonaws.com:5432/dd5rk21rc395se?user=qnbfniesbkxoxq&password=5a4b6bd7099b9154d9213fa971a1f43624f3379f31cadc02b4b971a010c9b689&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory" #_"datomic:dev://localhost:4334/stunners")

(defstate conn
  :start (do (d/create-database db-uri)
             (let [connection (d/connect db-uri)]
               @(d/transact connection schema)
               connection))
  :stop (.release conn))
