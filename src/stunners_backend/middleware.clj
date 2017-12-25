(ns stunners-backend.middleware
  (:require [clojure.string :as str]
            [jerks-whistling-tunes.core :as jwt]
            [jerks-whistling-tunes.sign :as sign]))

(defn authenticate [handler credentials]
  "Authenticates using auth token in Authorization header, or query param if header not set. Adds the auth token onto the request as :token, and the user's auth0 id as :user/auth0-id"
  (fn [request]
    (let [{:keys [headers query-params]} request
          token (-> (get headers "authorization")
                    (or "")
                    (str/replace #"^\s*Bearer\s+" ""))
          decoded-token (jwt/validate token
                                      (jwt/signature (sign/hs256 (:client-secret credentials)))
                                      (jwt/aud (:client-id credentials))
                                      (jwt/iss (:domain credentials))
                                      jwt/exp)]
      (if-let [{:keys [sub]} decoded-token]
        (handler (-> request
                     (assoc :token token)
                     (assoc :user/auth0-id sub)))
        {:status 401
         :headers {"Content-Type" "application/edn"}
         :body (pr-str {:message "Auth token not found or is invalid"})}))))
