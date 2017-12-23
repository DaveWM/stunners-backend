(ns stunners-backend.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def :request/user (s/keys :req [:user/name :user/email :user/avatar :user/phone-number :user/auth0-id]
                             :opt [:location/address]))

(s/def :user/name (s/and string? (complement str/blank?)))

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def :user/email (s/and string? #(re-matches email-regex %)))

(s/def :user/avatar (s/and string? #(-> (java.net.URI. %)
                                        .getScheme
                                        #{"http" "https"})))

(defn valid-phone-number? [phone-number-string]
  (->> (str/replace phone-number-string #"\s" "")
       (re-matches #"\d{11}")))
(s/def :user/phone-number (s/and string? valid-phone-number?))

(s/def :location/address (s/and string? (complement str/blank?)))

(s/def :user/auth0-id (s/and string? (complement str/blank?)))
