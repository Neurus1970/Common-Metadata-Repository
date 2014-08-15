(ns cmr.search.services.acl-service
  "Performs ACL related tasks for the search application"
  (:require [cmr.transmit.echo.tokens :as echo-tokens]
            [cmr.search.models.query :as qm]))

(defmulti add-acl-conditions-to-query
  "Adds conditions to the query to enforce ACLs"
  (fn [context query]
    (:concept-type query)))

(defmethod add-acl-conditions-to-query :granule
  [context query]
  ;; implement this in a future sprint
  query)

(defmethod add-acl-conditions-to-query :collection
  [context query]
  (let [{:keys [token]} context
        group-ids (if token
                    (map #(if (keyword? %) (name %) %)
                         (echo-tokens/get-current-sids context token))
                    ["guest"])
        acl-cond (qm/string-conditions :permitted-group-ids group-ids true)]
    (update-in query [:condition] #(qm/and-conds [acl-cond %]))))

