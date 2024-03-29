(defproject nl.jomco/ring-openapi-validator "0.1.2-SNAPSHOT"
  :description "Validate ring requests and responses against Swagger/OpenAPI"
  :url "https://github.com/zeekat/ring-openapi-validator"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [cheshire "5.10.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [com.atlassian.oai/swagger-request-validator-core "2.18.1"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  ;; link to git repo, also ensures that cljdoc.org finds additional markdown files
  :scm {:name "git"
        :url  "https://git.sr.ht/~jomco/ring-openapi-validator"})
