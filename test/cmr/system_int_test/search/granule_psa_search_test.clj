(ns cmr.system-int-test.search.granule-psa-search-test
  "Tests searching for granules by product specific attributes."
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.utils.search-util :as search]
            [cmr.system-int-test.utils.index-util :as index]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.granule :as dg]
            [cmr.system-int-test.data2.core :as d]
            [cmr.search.services.messages.attribute-messages :as am]))

(use-fixtures :each (ingest/reset-fixture "PROV1"))

(comment
  (ingest/create-provider "PROV1")
  )


(deftest invalid-psa-searches
  (are [v error]
       (= {:status 422 :errors [error]}
          (search/get-search-failure-data (search/find-refs :granule {"attribute[]" v})))
       ",alpha,a" (am/invalid-type-msg "")
       "foo,alpha,a" (am/invalid-type-msg "foo")
       ",alpha,a,b" (am/invalid-type-msg "")
       "string,,a" (am/invalid-name-msg "")
       "string,,a,b" (am/invalid-name-msg "")
       "string,alpha," (am/invalid-value-msg :string "")
       "string,alpha" (am/invalid-num-parts-msg)
       "string,alpha,," (am/one-of-min-max-msg)
       "string,alpha,b,a" (am/max-must-be-greater-than-min-msg "b" "a")
       "string,alpha,b,b" (am/max-must-be-greater-than-min-msg "b" "b")
       "float,alpha,a" (am/invalid-value-msg :float "a")
       "float,alpha,a,0" (am/invalid-value-msg :float "a")
       "float,alpha,0,b" (am/invalid-value-msg :float "b")
       "int,alpha,a" (am/invalid-value-msg :int "a")
       "int,alpha,a,0" (am/invalid-value-msg :int "a")
       "int,alpha,0,b" (am/invalid-value-msg :int "b"))

    (is (= {:status 422 :errors [(am/attributes-must-be-sequence-msg)]}
         (search/get-search-failure-data
           (search/find-refs :granule {"attribute" "string,alpha,a"})))))


(deftest string-psas-search-test
  (let [psa1 (dc/psa "alpha" :string)
        psa2 (dc/psa "bravo" :string)
        psa3 (dc/psa "charlie" :string)
        coll1 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa1 psa2]}))
        gran1 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "alpha" ["ab" "bc"])
                                                                                 (dg/psa "bravo" ["cd" "bf"])]}))
        gran2 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "bravo" ["ab"])]}))

        coll2 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa2 psa3]}))
        gran3 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "bravo" ["aa" "bf"])]}))
        gran4 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "charlie" ["az"])]}))]
    (index/flush-elastic-index)

    ;; TODO add additional deftest for datetime_string date_string, and time_string that they're treated as strings.

    (testing "search by value"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))
           "string,alpha,ab" [gran1]
           "string,alpha,c" []
           "string,bravo,bf" [gran1 gran3]))

    (testing "search by range"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))

           ;; inside range
           "string,alpha,aa,ac" [gran1]
           ;; beginning edge of range
           "string,alpha,ab,ac" [gran1]
           ;; ending edge of range
           "string,alpha,aa,ab" [gran1]

           ;; only min range provided
           "string,bravo,bc," [gran1 gran3]

           ;; only max range provided
           "string,bravo,,bc" [gran2 gran3]))))

(deftest float-psas-search-test
  (let [psa1 (dc/psa "alpha" :float)
        psa2 (dc/psa "bravo" :float)
        psa3 (dc/psa "charlie" :float)
        coll1 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa1 psa2]}))
        gran1 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "alpha" [10.5 123])
                                                                                 (dg/psa "bravo" [-12])]}))
        gran2 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "bravo" [10.5 123])]}))

        coll2 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa1 psa2 psa3]}))
        gran3 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "alpha" [14])
                                                                                 (dg/psa "bravo" [13.7 123])]}))
        gran4 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "charlie" [14])]}))]
    (index/flush-elastic-index)

    (testing "search by value"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))
           "float,bravo,123" [gran2, gran3]
           "float,alpha,10" []
           "float,bravo,-12" [gran1]))

    (testing "search by range"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))

           ;; inside range
           "float,alpha,10.2,11" [gran1]
           ;; beginning edge of range
           "float,alpha,10.5,10.6" [gran1]
           ;; ending edge of range
           "float,alpha,10,10.5" [gran1]

           ;; only min range provided
           "float,bravo,120," [gran2 gran3]

           ;; only max range provided
           "float,bravo,,13.6" [gran1 gran2]))))

(deftest int-psas-search-test
  (let [psa1 (dc/psa "alpha" :int)
        psa2 (dc/psa "bravo" :int)
        psa3 (dc/psa "charlie" :int)
        coll1 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa1 psa2]}))
        gran1 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "alpha" [10 123])
                                                                                 (dg/psa "bravo" [-12])]}))
        gran2 (d/ingest "PROV1" (dg/granule coll1 {:product-specific-attributes [(dg/psa "bravo" [10 123])]}))

        coll2 (d/ingest "PROV1" (dc/collection {:product-specific-attributes [psa1 psa2 psa3]}))
        gran3 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "alpha" [14])
                                                                                 (dg/psa "bravo" [13 123])]}))
        gran4 (d/ingest "PROV1" (dg/granule coll2 {:product-specific-attributes [(dg/psa "charlie" [14])]}))]
    (index/flush-elastic-index)

    (testing "search by value"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))
           "int,bravo,123" [gran2, gran3]
           "int,alpha,11" []
           "int,bravo,-12" [gran1]))

    (testing "search by range"
      (are [v items]
           (d/refs-match? items (search/find-refs :granule {"attribute[]" v}))

           ;; inside range
           "int,alpha,9,11" [gran1]
           ;; beginning edge of range
           "int,alpha,10,11" [gran1]
           ;; ending edge of range
           "int,alpha,9,10" [gran1]

           ;; only min range provided
           "int,bravo,120," [gran2 gran3]

           ;; only max range provided
           "int,bravo,,12" [gran1 gran2]))))





