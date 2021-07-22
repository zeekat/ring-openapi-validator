(ns nl.zeekat.ring-openapi-validator-test
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [nl.zeekat.ring-openapi-validator :as validator]))

(def ooapi-content-type "application/hal+json; charset=utf-8")

(def ooapi-request {:request-method :get
                    :uri            "/"})

(def ooapi-invalid-request
  {:request-method :post
   :uri            "/"})

(def ooapi-resource {:documentation "https://example.com/documentation",
                     :roomTypes     ["General purpose" "Lecture hall" "PC lab"],
                     :specification "https://example.com/specification",
                     :courseLevels  ["Bachelor" "Master"],
                     :logo          "https://example.com/logo.png",
                     :owner         "Academie van Vierakker",
                     :_links
                     {:self {:href "/"},
                      :endpoints
                      [{:href "/institution"}
                       {:href "/educational-programmes"}
                       {:href "/course-offerings"}
                       {:href "/persons"}
                       {:href "/courses"}]}})

(def ooapi-response  {:headers {"Content-Type" ooapi-content-type}
                      :status  200
                      :body    (json/json-str ooapi-resource)})

(def ooapi-invalid-response  {:headers {"Content-Type" ooapi-content-type}
                              :status  200
                              :body    (json/json-str (dissoc ooapi-resource :owner))})

(deftest test-openapi-validator
  (let [validator (validator/openapi-validator "ooapi.json" {})]
    (is (instance? com.atlassian.oai.validator.OpenApiInteractionValidator validator))
    (testing "interaction"
      (let [report (validator/validate-interaction validator
                                                   ooapi-request
                                                   ooapi-response)]
        (is (nil? report) "nil report when no errors are reported"))
      (let [report (validator/validate-interaction validator
                                                   ooapi-request
                                                   ooapi-invalid-response)]
        (is (= "validation.response.body.schema.required"
               (get-in report [0 :key]))
            "Report on missing property")))
    (testing "request"
      (is (nil? (validator/validate-request validator ooapi-request)))
      (is (= "validation.request.operation.notAllowed"
             (get-in (validator/validate-request validator ooapi-invalid-request) [0 :key]))
          "Report on invalid request"))

    (testing "response"
      (is (nil? (validator/validate-response validator :get "/" ooapi-response)))
      (is (= "validation.response.body.schema.required"
             (get-in (validator/validate-response validator :get "/" ooapi-invalid-response) [0 :key]))
          "Report on invalid request"))))

(deftest test-inline-validator
  (is (instance? com.atlassian.oai.validator.OpenApiInteractionValidator
                 (validator/openapi-validator (slurp (io/resource "ooapi.json")) {:inline? true}))))
