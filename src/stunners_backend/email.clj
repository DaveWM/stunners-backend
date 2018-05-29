(ns stunners-backend.email
  (:require [postal.core :as postal]
            [environ.core :refer [env]]))

(def sender-address "stunners.app@gmail.com")

(def config {:host "smtp.gmail.com"
             :ssl true
             :user sender-address
             :pass (env :email-password)})

(defn send! [to-address subject body]
  (postal/send-message config {:from sender-address
                               :to to-address
                               :subject subject
                               :body body}))