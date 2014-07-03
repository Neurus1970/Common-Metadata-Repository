(ns cmr.system-int-test.utils.transformer
  "Helper functions for transformer integration tests."
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [clojure.string :as str]
            [cheshire.core :as json]
            [cmr.common.concepts :as cs]
            [cmr.system-int-test.utils.url-helper :as url]
            [cmr.common.util :as u]
            [cmr.common.mime-types :as mt]
            [camel-snake-kebab :as csk]
            [clojure.walk]
            [ring.util.codec :as codec]
            [cmr.umm.core :as ummc]))

(defn transform-concepts
  "Transform concepts given as concept-id, revision tuples into the given format"
  [umm-records format]
  (let [mime-type (format mt/format->mime-type)
        tuples (map #(vector (:concept-id %) (:revision-id %)) umm-records)
        response (client/post (str (url/transformer-url) "/concepts")
                              {:accept mime-type
                               :throw-exceptions false
                               :content-type "application/json"
                               :body (json/encode tuples)})
        status (:status response)
        parsed (json/decode (:body response) true)]
    {:status status :response parsed}))

(defn transform-latest-concepts
  "Transform latest version of concepts given as concept-id into the given format"
  [ids format]
  (let [mime-type (format mt/format->mime-type)
        response (client/post (str (url/transformer-url) "/latest-concepts")
                              {:accept mime-type
                               :throw-exceptions false
                               :content-type "application/json"
                               :body (json/encode ids)})
        status (:status response)
        parsed (json/decode (:body response) true)]
    {:status status :response parsed}))

(defn expected-response
  "Returns the expected xml for a given vector of umm records"
  [umm-records format]
  (map #(ummc/umm->xml % format) umm-records))
