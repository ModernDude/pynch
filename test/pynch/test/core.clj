(ns pynch.test.core
  (:use [pynch.core])
  (:use [clojure.test])
  (:require [clj-time.core :as dt]))

(deftest test-re-first-seq-digits
  (testing "Number Exist"
    (testing "Numbers Alone"
      (is (= 23 (re-first-seq-digits "23"))))
    (testing "Numbers in front of text"
      (is (= 23 (re-first-seq-digits "23 is a big number"))))
    (testing "Numbers at end of text"
      (is (= 23 (re-first-seq-digits "is a big 23 number"))))
    (testing "First Number Of Two"
      (is (= 23 (re-first-seq-digits "23 is a 24"))))
    (testing "First Number Of Two With Text At Front"
      (is (= 24 (re-first-seq-digits "Some Number 24 23"))))
    (testing "Single Digit Number"
      (is (= 1 (re-first-seq-digits "1")))))
  (testing "Number Not Exist"
    (testing "When No Default Specified, Return null"
      (is (nil? (re-first-seq-digits ""))))
    (testing "When Default Specified, Default returned"
      (is (= 20 (re-first-seq-digits "", 20))))))

(def test-dt (dt/date-time 1998 4 25))



(defn dates-same? [d1 d2]
  (and (= (dt/year d1) (dt/year d2))
       (= (dt/month d1) (dt/month d2))
       (= (dt/day d1) (dt/day d2))
       (= (dt/hour d1) (dt/hour d2))
       (= (dt/minute d1) (dt/minute d2))))


(deftest test-hn-time-to-date
  (testing "Time Period Specificed"
    (testing "Years"
      (is (dates-same?  (dt/minus (dt/now) (dt/years 10)) (hn-time-to-dt "10 years ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/years 10)) (hn-time-to-dt "10 year ago"))))
    (testing "Months"
      (is (dates-same?  (dt/minus (dt/now) (dt/months 4)) (hn-time-to-dt "4 months ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/months 4)) (hn-time-to-dt "4 months ago"))))
    (testing "Weeks"
      (is (dates-same?  (dt/minus (dt/now) (dt/weeks 4)) (hn-time-to-dt "4 weeks ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/weeks 4)) (hn-time-to-dt "4 week ago"))))
     (testing "Days"
      (is (dates-same?  (dt/minus (dt/now) (dt/days 4)) (hn-time-to-dt "4 days ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/days 4)) (hn-time-to-dt "4 day ago"))))
     (testing "Hours"
      (is (dates-same?  (dt/minus (dt/now) (dt/hours 4)) (hn-time-to-dt "4 hours ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/hours 4)) (hn-time-to-dt "4 hour ago"))))
     (testing "Minutes"
      (is (dates-same?  (dt/minus (dt/now) (dt/minutes 4)) (hn-time-to-dt "4 minutes ago")))
      (is (dates-same?  (dt/minus (dt/now) (dt/minutes 4)) (hn-time-to-dt "4 minute ago")))))
  (testing "Time Period Not Specified"
    (is (dates-same? (dt/now) (hn-time-to-dt ""))))
