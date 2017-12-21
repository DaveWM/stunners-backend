(defproject stunners-backend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha19"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.2.1"]
                 [org.clojure/core.async "0.3.465"]
                 [com.datomic/clj-client "0.8.606"]
                 [org.eclipse.jetty/jetty-server "9.3.7.v20160115"]
                 [ring "1.6.0" :exclusions [org.eclipse.jetty/jetty-server]]]
  :plugins [[lein-ring "0.12.2"]]
  :ring {:handler stunners-backend.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
