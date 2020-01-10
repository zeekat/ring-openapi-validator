(defproject nl.zeekat/ring-openapi-validator "0.1.0"
  :description "Validate ring requests and responses against Swagger/OpenAPI"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [com.atlassian.oai/swagger-request-validator-core "2.8.3"]]
  :profiles {:dev {:dependencies [[org.clojure/data.json "0.2.7"]]
                   :resource-paths ["dev-resources"]}})

