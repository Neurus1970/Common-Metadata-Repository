(ns cmr.system-int-test.search.paging-search-test
  "Tests for search paging."
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]))

(def collection-count 25)

(defn setup
  "set up the fixtures for test"
  []
  (ingest/reset)
  (let [provider-id "PROV1"]
    (ingest/create-provider provider-id)
    (doseq [seq-num (range 1 (inc collection-count))]
      (ingest/update-collection provider-id (search/collection-concept provider-id seq-num))))
  (index/flush-elastic-index))

(defn teardown
  "tear down after the test"
  []
  (ingest/reset))

(defn wrap-setup
  [f]
  (try
    (f)
    (finally (teardown))))

(use-fixtures :each wrap-setup)

(deftest search-with-page-size
  (setup)
  (testing "Search with page size."
    (let [references (search/find-refs :collection {:provider "PROV1"
                                                   :page_size 5})]
      (is (= 5 (count references)))))
  (testing "Search with large page size."
    (let [references (search/find-refs :collection {:provider "PROV1"
                                                   :page_size 100})]
      (is (= collection-count (count references)))))
  (testing "Page size less than one."
    (try
      (search/find-refs :collection {:provider "PROV1"
                                    :page_size 0})
      (catch clojure.lang.ExceptionInfo e
        (let [status (get-in (ex-data e) [:object :status])
              body (get-in (ex-data e) [:object :body])]
          (is (= 422 status))
          (is (re-matches #".*page_size must be a number between 1 and 2000.*" body))))))
  (testing "Negative page size."
    (try
      (search/find-refs :collection {:provider "PROV1"
                                    :page_size -1})
      (catch clojure.lang.ExceptionInfo e
        (let [status (get-in (ex-data e) [:object :status])
              body (get-in (ex-data e) [:object :body])]
          (is (= 422 status))
          (is (re-matches #".*page_size must be a number between 1 and 2000.*" body))))))
  (testing "Page size too large."
    (try
      (search/find-refs :collection {:provider "PROV1"
                                    :page_size 2001})
      (catch clojure.lang.ExceptionInfo e
        (let [status (get-in (ex-data e) [:object :status])
              body (get-in (ex-data e) [:object :body])]
          (is (= 422 status))
          (is (re-matches #".*page_size must be a number between 1 and 2000.*" body))))))
  (testing "Non-numeric page size"
    (try
      (search/find-refs :collection {:provider "PROV1"
                                    :page_size "ABC"})
      (catch clojure.lang.ExceptionInfo e
        (let [status (get-in (ex-data e) [:object :status])
              body (get-in (ex-data e) [:object :body])]
          (is (= 422 status))
          (is (re-matches #".*page_size must be a number between 1 and 2000.*" body)))))))

(deftest search-with-page-num
  (setup)
  (testing "Search with page num."
    (let [references (search/find-refs :collection {:provider "PROV1"
                                                   :page_size 5
                                                   :page_num 1})]
      (is (= 5 (count references))))))
