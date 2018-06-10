(ns stunners-backend.config
  (:require [aero.core :refer [read-config]]
            [mount.core :refer [defstate]]))


(defstate config
  :start (read-config "resources/config.edn"))
