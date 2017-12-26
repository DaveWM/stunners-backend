(defproject stunners-backend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username [:gpg :env/datomic_username]
                                   :password [:gpg :env/datomic_password]}}
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.2.1"]
                 [org.clojure/core.async "0.3.465"]
                 [com.datomic/datomic-pro "0.9.5656"]
                 [org.eclipse.jetty/jetty-server "9.3.7.v20160115"]
                 [ring "1.6.0" :exclusions [org.eclipse.jetty/jetty-server]]
                 [jerks-whistling-tunes "0.2.4"]
                 [fogus/ring-edn "0.3.0"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [clj-http "3.7.0"]
                 [cheshire "5.8.0"]
                 [metosin/spec-tools "0.5.1"]
                 [mount "0.1.11"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.82"]]
  :plugins [[lein-ring "0.12.2"]]
  :ring {:handler stunners-backend.handler/app
         :init stunners-backend.handler/init
         :nrepl {:start? true}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
