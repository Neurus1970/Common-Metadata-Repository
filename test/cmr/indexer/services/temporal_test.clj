(ns cmr.indexer.services.temporal-test
  "Tests for temporal to start-date/end-date conversion"
  (:require [clojure.test :refer :all]
            [cmr.umm.collection :as c]
            [clj-time.core :as t]
            [cmr.indexer.services.temporal :as tm]))

(def dt1 (t/date-time 2001 10 15 4 3 27 1))
(def dt2 (t/date-time 2002 10 15 4 3 27 2))
(def dt3 (t/date-time 2003 10 15 4 3 27 3))
(def dt4 (t/date-time 2004 10 15 4 3 27 4))

(def range1 (c/map->RangeDateTime {:beginning-date-time dt1
                                   :ending-date-time dt2}))
(def range2 (c/map->RangeDateTime {:beginning-date-time dt3
                                   :ending-date-time dt4}))
(def range3 (c/map->RangeDateTime {:beginning-date-time dt3
                                   :ending-date-time nil}))

(def periodic1 (c/map->PeriodicDateTime {:start-date dt1
                                         :end-date dt2}))
(def periodic2 (c/map->PeriodicDateTime {:start-date dt3
                                         :end-date dt4}))

(deftest start-date-test
  (testing "range date times only"
    (let [temporal (c/map->TemporalCoverage {:range-date-times [range1 range2]})]
      (is (= dt1 (tm/start-date :collection temporal)))))
  (testing "single date times only"
    (let [temporal (c/map->TemporalCoverage {:single-date-times [dt2 dt3 dt4]})]
      (is (= dt2 (tm/start-date :collection temporal)))))
  (testing "periodic date times only"
    (let [temporal (c/map->TemporalCoverage {:periodic-date-times [periodic1 periodic2]})]
      (is (= dt1 (tm/start-date :collection temporal)))))
  ;; no need to test for mixed datetimes as only one of the three types of the datetimes will exist in a temporal coverage
  )

(deftest end-date-test
  (testing "range date times without ends-at-present-flag"
    (let [temporal (c/map->TemporalCoverage {:range-date-times [range1 range2 range3]})]
      (is (= dt4 (tm/end-date :collection temporal)))))
  (testing "range date times with ends-at-present-flag"
    (let [temporal (c/map->TemporalCoverage {:ends-at-present-flag true
                                             :range-date-times [range1 range2 range3]})]
      (is (= nil (tm/end-date :collection temporal)))))
  (testing "single date times only"
    (let [temporal (c/map->TemporalCoverage {:single-date-times [dt2 dt3 dt4]})]
      (is (= dt4 (tm/end-date :collection temporal)))))
  (testing "periodic date times only"
    (let [temporal (c/map->TemporalCoverage {:periodic-date-times [periodic1 periodic2]})]
      (is (= dt4 (tm/end-date :collection temporal)))))
  ;; no need to test for mixed datetimes as only one of the three types of the datetimes will exist in a temporal coverage
  )
