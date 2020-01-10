(ns nl.zeekat.ring-swagger-validator
  (:require [clojure.string :as string])
  (:import com.atlassian.oai.validator.OpenApiInteractionValidator
           com.atlassian.oai.validator.SwaggerRequestResponseValidator
           com.atlassian.oai.validator.model.Request
           com.atlassian.oai.validator.model.Request$Method
           com.atlassian.oai.validator.model.Response
           com.atlassian.oai.validator.report.ValidationReport
           com.atlassian.oai.validator.report.ValidationReport$Level
           com.atlassian.oai.validator.report.ValidationReport$MessageContext$Location
           java.util.Optional))

(def ^:private ring->Method
  {:get     Request$Method/GET
   :post    Request$Method/POST
   :put     Request$Method/PUT
   :patch   Request$Method/PATCH
   :delete  Request$Method/DELETE
   :head    Request$Method/HEAD
   :options Request$Method/OPTIONS
   :trace   Request$Method/TRACE})

(defn- ->coll
  [x]
  (cond
    (or (nil? x) (= "" x))
    []
    (coll? x)
    x
    :else
    [x]))

(defn- normalize-headers
  [headers]
  (reduce-kv (fn [m k v]
               (assoc m (string/lower-case k) (->coll v)))
             {}
             headers))

(defn- ring->Request
  [{:keys [uri request-method body query-params headers]}]
  (let [headers (normalize-headers headers)]
    (proxy [Request] []
      (getPath []
        uri)
      (getMethod []
        (ring->Method request-method))
      (getBody []
        (Optional/ofNullable body))
      (getQueryParameters []
        (->> query-params
             keys
             (map name)))
      (getQueryParameterValues [n]
        (->coll (get query-params n)))
      (getHeaders []
        headers)
      (getHeaderValues [n]
        (->coll (get headers (string/lower-case n))))
      (getHeaderValue [n]
        (Optional/ofNullable (first (.getHeaderValues this n))))
      (getContentType []
        (.getHeaderValue this "content-type")))))

(defn- ring->Response
  [{:keys [status body headers] :as response}]
  (let [headers (normalize-headers headers)]
    (proxy [Response] []
      (getStatus []
        status)
      (getBody []
        (Optional/ofNullable body))
      (getHeaderValues [n]
        (->coll (get headers (string/lower-case n))))
      (getHeaderValue [n]
        (Optional/ofNullable (first (.getHeaderValues this n))))
      (getContentType []
        (.getHeaderValue this "content-type")))))

(def ^:private Level->key
  {ValidationReport$Level/ERROR  :error
   ValidationReport$Level/IGNORE :ignore
   ValidationReport$Level/INFO   :info
   ValidationReport$Level/WARN   :warn})

(defn- Message->map
  [msg]
  {:key             (.getKey msg)
   :message         (.getMessage msg)
   :level           (Level->key (.getLevel msg))
   :additional-info (.getAdditionalInfo msg)
   :nested-messages (map Message->map (.getNestedMessages msg))})

(def ^:private Location->key
  {ValidationReport$MessageContext$Location/REQUEST  :request
   ValidationReport$MessageContext$Location/RESPONSE :response})

(defn- Context->map
  [ctx]
  ;; TODO
  )

(defn- report->coll
  [report]
  (let [coll (mapv Message->map (.getMessages report))]
    (when (seq coll)
      coll)))

(defn validate-interaction
  [validator request response]
  (report->coll (.validate validator (ring->Request request) (ring->Response response))))

(defn validate-request
  [validator request]
  (report->coll (.validateRequest validator (ring->Request request))))

(defn validate-response
  [validator method path response]
  (report->coll (.validateResponse validator path (ring->Method method) (ring->Response response))))

(defn openapi-validator
  [spec {:keys [base-path]}]
  (cond-> (OpenApiInteractionValidator/createFor spec)
    base-path
    (.withBasePathOverride base-path)
    true
    (.build)))

(defn swagger-validator
  [spec {:keys [base-path]}]
  (cond-> (SwaggerRequestResponseValidator/createFor spec)
    base-path
    (.withBasePathOverride base-path)
    true
    (.build)))

