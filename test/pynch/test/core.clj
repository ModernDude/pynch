(ns pynch.test.core
  (:use [pynch.core])
  (:use [clojure.test])
  (:require [clj-time.core :as dt])
  (:use [clojure.java.io :as io]))

(deftest test-get-subs
  (let [c (-> "resources/submissions.html" io/resource get-subs)]
    (testing "Count"
      (is (= 30 (count c ))) )
    (testing "First Submission"
      (let [sub (first c)]
        (is (= 1 (:ordinal sub)))
        (is (= "Why Geeks Should Love HP WebOS" (:title sub)))
        (is (= "http://developer.palm.com/blog/2011/05/10-reasons-for-geeks-to-love-hp-webos/"
               (:submission-url sub)))
        (is (= (hn-time-to-dt "3 hours ago") (:submission-time sub)))
        (is (= 118 (:points sub)))
        (is (= "unwiredben" (:user sub)))
        (is (= "item?id=2538655" (:comments-url sub)))
        (is (= 49 (:comments-count sub)))))
    (testing "Submission with no points and no comments"
      (let [sub (nth c 14)]
        (is (= 15 (:ordinal sub)))
        (is (= "Facebook reportedly disables account of attorney Mark S. Zuckerberg"
               (:title sub)))
        (is (= "http://latimesblogs.latimes.com/technology/2011/05/facebook-disables-account-of-attorney-named-mark-zuckerberg.html"
               (:submission-url sub)))
        (is (= (hn-time-to-dt "4 hours ago") (:submission-time sub)))
        (is (= 0 (:points sub)))
        (is (= "ssclafani" (:user sub)))
        (is (= "item?id=2538477" (:comments-url sub)))
        (is (= 0 (:comments-count sub)))))
    (testing "Last Submission"
      (let [sub (last c)]
        (is (= 30 (:ordinal sub)))
        (is (= "Finally, Google Tasks API", (:title sub)))
        (is (= "https://code.google.com/apis/tasks/index.html"
               (:submission-url sub)))
        (is (= (hn-time-to-dt "6 hours ago") (:submission-time sub)))
        (is (= 32 (:points sub)))
        (is (= "toomanymike" (:user sub)))
        (is (= "item?id=2538023"))
        (is (= 4 (:comments-count sub)))))))

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

(defn dates-same? [d1 d2]
  (and (= (dt/year d1) (dt/year d2))
       (= (dt/month d1) (dt/month d2))
       (= (dt/day d1) (dt/day d2))
       (= (dt/hour d1) (dt/hour d2))
       (= (dt/minute d1) (dt/minute d2))))

(defn now-minus []
  "Returns a function that will subtract periods from the
   current date"
  (partial dt/minus (dt/now)))

(deftest test-hn-time-to-date
  (testing "Time Period Specificed"
    (testing "Years"
      (is (dates-same? ((now-minus) (dt/years 10)) (hn-time-to-dt "10 years ago")))
      (is (dates-same? ((now-minus) (dt/years 10)) (hn-time-to-dt "10 year ago"))))
    (testing "Months"
      (is (dates-same? ((now-minus) (dt/months 4)) (hn-time-to-dt "4 months ago")))
      (is (dates-same? ((now-minus) (dt/months 4)) (hn-time-to-dt "4 months ago"))))
    (testing "Weeks"
      (is (dates-same? ((now-minus) (dt/weeks 4)) (hn-time-to-dt "4 weeks ago")))
      (is (dates-same? ((now-minus) (dt/weeks 4)) (hn-time-to-dt "4 week ago"))))
     (testing "Days"
      (is (dates-same? ((now-minus) (dt/days 4)) (hn-time-to-dt "4 days ago")))
      (is (dates-same? ((now-minus) (dt/days 4)) (hn-time-to-dt "4 day ago"))))
     (testing "Hours"
      (is (dates-same? ((now-minus) (dt/hours 4)) (hn-time-to-dt "4 hours ago")))
      (is (dates-same? ((now-minus) (dt/hours 4)) (hn-time-to-dt "4 hour ago"))))
     (testing "Minutes"
      (is (dates-same? ((now-minus) (dt/minutes 4)) (hn-time-to-dt "4 minutes ago")))
      (is (dates-same? ((now-minus) (dt/minutes 4)) (hn-time-to-dt "4 minute ago")))))
  (testing "Time Period Not Specified"
    (is (dates-same? (dt/now) (hn-time-to-dt "")))))

