(defproject nl.zeekat/ring-openapi-validator "0.1.1"
  :description "Validate ring requests and responses against Swagger/OpenAPI"
  :url "https://github.com/zeekat/ring-openapi-validator"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [com.atlassian.oai/swagger-request-validator-core "2.18.1"]]
  :profiles {:dev {:dependencies [[org.clojure/data.json "2.4.0"]]
                   :resource-paths ["dev-resources"]
                   :plugins [[lein-codox "0.10.7"]]
                   :codox {:metadata {:doc/format :markdown}
                           :output-path "codox"}}}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]])

