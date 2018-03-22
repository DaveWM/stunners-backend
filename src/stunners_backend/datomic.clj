(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]
            [mount.core :refer [defstate]]
            [stunners-backend.schema :refer [schema]]))


(def db-uri "datomic:sql://stunners?jdbc:postgresql://ec2-79-125-110-209.eu-west-1.compute.amazonaws.com:5432/d4buk3vuuj5442?user=qwjavacktkbhgo&password=654b207e5679f6cbd9684eb3411b82d16c4dc831d1a7cd213a7d48495ed4c883&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory" #_"datomic:dev://localhost:4334/stunners")

(defstate conn
  :start (do (d/create-database db-uri)
             (let [connection (d/connect db-uri)]
               @(d/transact connection schema)
               connection))
  :stop (.release conn))
