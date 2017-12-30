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
                                      (jwt/aud [(:client-id credentials)])
                                      (jwt/iss (:domain credentials))
                                      jwt/exp)]
      (if-let [{:keys [sub]} decoded-token]
        (handler (-> request
                     (assoc :token token)
                     (assoc :user/auth0-id sub)))
        {:status 401
         :body {:message "Auth token not found or is invalid"}}))))

(defn edn [handler]
  (fn [request]
    (-> (handler request)
        (update :body pr-str)
        (update :headers (partial merge {"Content-Type" "application/edn"})))))

(defn handle-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        {:status 500
         :body {:message (str "Error: " (.getMessage e))}}))))

(defn wrap-cors
  "Allow requests from all origins"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Credentials"] "true")))))
