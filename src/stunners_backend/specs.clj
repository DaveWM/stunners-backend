(ns stunners-backend.specs
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.string :as str]))

(s/def :request/user (st/spec (s/keys :req [:user/name :user/email :user/avatar :user/phone-number :user/auth0-id]
                                      :opt [:location/address])))

(s/def :request/appointment (st/spec (s/keys :req [:location/lat :location/lng :appointment/stylist :appointment/time :appointment/product-types])))

(s/def :request/appointment-update (st/spec (s/keys :req [:appointment/status])))

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

(s/def :appointment/status #{:appointment-status/pending :appointment-status/confirmed :appointment-status/rejected})

(s/def :appointment/product-types (s/coll-of #{:product-type/haircut :product-type/waxing :product-type/nails} :min-count 1 :distinct true))

(s/def :appointment/time inst?)

(s/def :location/lat (s/and double? #(<= -90 % 90)))
(s/def :location/lng (s/and double? #(<= -180 % 180)))

(s/def :appointment/stylist pos-int?)

(s/def :appointment/status #{:appointment-status/pending :appointment-status/confirmed :appointment-status/rejected})
