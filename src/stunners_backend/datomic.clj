(ns stunners-backend.datomic
  (:require [datomic.api :as d]
            [clojure.core.async :refer [<!!]]))


(def db-uri "datomic:dev://localhost:4334/hello")

(def conn (d/connect db-uri))
