(ns stunners-backend.specs
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.string :as str]
            [stunners-backend.enums :as enums]))

(s/def :coeffects/current-time inst?)

(s/def :request/coeffects (s/keys :req [:coeffects/current-time]))

(s/def :request/user (st/spec (s/keys :req [:user/name :user/email :user/phone-number]
                                      :opt [:location/address :user/avatar])))

(s/def :request/appointment (st/spec (s/and (s/merge (s/keys :req [:location/lat :location/lng :appointment/stylist :appointment/time :appointment/product-types])
                                                     :request/coeffects)
                                            (fn [{:keys [appointment/time coeffects/current-time]}]
                                              (.before current-time time)))))

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

(s/def :appointment/product-types (s/coll-of enums/product-type :min-count 1 :distinct true))

(s/def :appointment/time inst?)

(s/def :location/lat (s/and double? #(<= -90 % 90)))
(s/def :location/lng (s/and double? #(<= -180 % 180)))

(s/def :appointment/stylist pos-int?)

(s/def :appointment/status enums/appointment-status)
