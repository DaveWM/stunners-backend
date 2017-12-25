(ns stunners-backend.utils
  (:require [clojure.spec.alpha :as s]))

(defn inline-enums [query-result]
  (clojure.walk/prewalk (fn [x]
                          (if (and (map? x) (= (count x) 1))
                            (if-let [ident (:db/ident x)]
                              ident
                              x)
                            x))
                        query-result))

(defn split-main-related [query-result]
  (let [[main & related] (->> query-result
                              (apply map vector)
                              (map distinct))]
    {:main main
     :related (flatten related)}))

(defn spec-failed-response [spec-key entity]
  {:status 400
   :headers {"Content-Type" "application/edn"}
   :body (pr-str {:message "Invalid request"
                  :explanation (s/explain-data spec-key entity)})})
