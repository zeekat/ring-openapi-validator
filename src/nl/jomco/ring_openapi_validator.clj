(ns nl.jomco.ring-openapi-validator
  (:require [clojure.string :as string]
            [cheshire.core :as json])
  (:import com.atlassian.oai.validator.OpenApiInteractionValidator
           com.atlassian.oai.validator.model.Request
           com.atlassian.oai.validator.model.Request$Method
           com.atlassian.oai.validator.model.Response
           com.atlassian.oai.validator.model.StringBody
           com.atlassian.oai.validator.report.ValidationReport
           com.atlassian.oai.validator.report.ValidationReport$Level
           com.atlassian.oai.validator.report.ValidationReport$MessageContext$Location
           java.util.Optional
           java.nio.charset.Charset
           java.nio.charset.StandardCharsets))

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
    (reify Request
      (getPath [this]
        uri)
      (getMethod [this]
        (ring->Method request-method))
      (getBody [this]
        (Optional/ofNullable body))
      (getQueryParameters [this]
        (->> query-params
             keys
             (map name)))
      (getQueryParameterValues [this n]
        (->coll (get query-params n)))
      (getHeaders [this]
        headers)
      (getHeaderValues [this n]
        (->coll (get headers (string/lower-case n)))))))

(defn- ring->Response
  [{:keys [status body headers] :as response}]
  (let [headers (normalize-headers headers)]
    (reify Response
      (getStatus [this]
        status)
      (getBody [this]
        (Optional/ofNullable body))
      (getHeaderValues [this n]
        (->coll (get headers (string/lower-case n)))))))

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

(defn- report->coll
  [report]
  (let [coll (mapv Message->map (.getMessages report))]
    (when (seq coll)
      coll)))

(defn openapi-validator
  "Build an OpenApiInteractionValidator from a spec

  `spec` is a url or path to resource describing a Swagger or OpenApi
  specification.

  `opts` is an optional map of options:
   - `:base-path` overrides the base path in the spec.
   - `:inline? true` indicate that `spec` is the specification body
      as a string, instead of a url or path

  If you need to customize the validator you can create a builder using
  `com.atlassian.oai.validator.OpenApiInteractionValidator/createFor`"
  ([spec {:keys [base-path inline?] :as opts}]
   (cond-> (if inline?
             (OpenApiInteractionValidator/createForInlineApiSpecification spec)
             (OpenApiInteractionValidator/createFor spec))
     base-path
     (.withBasePathOverride base-path)
     true
     (.build)))
  ([spec]
   (openapi-validator spec {})))


(defn validate-interaction
  "Validate a `request`/`response` pair using the given `validator`.

  If any issues are found, returns a report collection"
  [validator request response]
  (report->coll (.validate validator (ring->Request request) (ring->Response response))))

(defn validate-request
  "Validate a `request` using the given `validator`.

  If any issues are found, returns a report collection"
  [validator request]
  (report->coll (.validateRequest validator (ring->Request request))))

(defn validate-response
  "Validate a `response` using the given `validator`.

  - `method` is a ring-spec method: `:get` `:head` `:post` etc...
  - `path` is the request path excluding parameters

  If any issues are found, returns a report collection"
  [validator method path response]
  (report->coll (.validateResponse validator path (ring->Method method) (ring->Response response))))


;; FIXME:
;; https://stackoverflow.com/questions/5034311/multiple-readers-for-inputstream-in-java/30262036#30262036
;; strategy: copy the input steam /once/ so that there can be exactly
;; one downstream reader, and the memory can be freed after - fully
;; immutable means there's no way to tell when we're "done" with the
;; request body.

(defn- immutable-request-body
  "Replace mutable InputStream body with the equivalent re-readable content.

  Ensures that any multiple handlers/middleware can read the body
  content without stepping on each other's toes.

  Returns a new ring request"
  [{:keys [body] :as request}]
  (if (instance? java.io.InputStream body)
    (assoc request :body (slurp body))
    request))

(defn wrap-request-validator
  "Middleware validating requests against an OOAPI spec.

    - `f` is the handler to wrap
    - `validator` should be an `OpenApiInteractionValidator` - see
      [[openapi-validator]].

  Each incoming request is validated using [[validate-request]]. When
  errors are found, the a 400 Bad Request response is returned with the
  error collection as the response body. When the request is valid
  according to the validator, it is passed along to the original
  handler.

      (-> my-api-handler
          (wrap-validator (openapi-validator \"path/to/spec.json\"))
          (wrap-json-response))

  Since the error response body is just clojure map, you need some other
  middleware like `ring.middleware.json` to turn it into full ring
  response."
  [f validator]
  (fn [request]
    (let [request (immutable-request-body request)]
      (if-let [errs (validate-request validator request)]
        {:status 400 ; bad request
         :body   errs}
        (f request)))))
