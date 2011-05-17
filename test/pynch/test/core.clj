(ns pynch.test.core
  (:use [pynch.core])
  (:use [clojure.test])
  (:require [clj-time.core :as dt])
  (:use [clojure.java.io :as io]))


(def sub-1
  (make-sub
   1 "Why Geeks Should Love HP WebOS"
   "http://developer.palm.com/blog/2011/05/10-reasons-for-geeks-to-love-hp-webos/"
   (hn-time-to-dt "3 hours ago") 118 "unwiredben" "item?id=2538655" 49))

(def sub-14
  (make-sub
   14 "Google: Go ahead and hack the Chrome Book"
   "http://techcrunch.com/2011/05/11/hack-chromebooks"
   (hn-time-to-dt "6 hours ago") 57 "MatthewB" "item?id=2537994" 0))

(def sub-30
  (make-sub
   30 "Finally, Google Tasks API"
   "https://code.google.com/apis/tasks/index.html"
   (hn-time-to-dt "6 hours ago") 32 "toomanymike" "item?id=2538023" 4))

(def sub-31
  (make-sub
   31 "The False Choice Between Babies And Startups"
   "http://blogs.forbes.com/85broads/2011/05/16/the-false-choice-between-babies-and-startups/"
   (hn-time-to-dt "12 hours ago") 46 "jemeshsu" "item?id=2554807" 30))

(def sub-60
  (make-sub
   60 "Exploring Lisp Libraries - and building a webapp on the way"
   "https://sites.google.com/site/sabraonthehill/home/exploring-quicklisp-packages"
   (hn-time-to-dt "1 day ago") 69 "mahmud" "item?id=2552163" 3))

(deftest test-get-subs
  (let [subs (-> "resources/submissions.html" io/resource get-subs)]
    (testing "Count is 30"
      (is (= 30 (count subs ))) )
    (testing "First Submission"
      (is (= sub-1 (first subs))))
    (testing "Fourteenth Submission"
      (is (= sub-14 (nth subs 13))))
    (testing "Thirtieth Submission"
      (is (= sub-30 (last subs))))))

(deftest test-get-subs-follow
  (let [subs (->> "resources/submissions.html" io/resource get-subs-follow (take 60))]
    (testing "Count is 60"
      (is (= 60 (count subs))))
    (testing "First Submission"
      (is (= sub-1 (first subs))))
    (testing "Thirty-First Submission"
      (is (= sub-31 (nth subs 30))))
    (testing "Sixtieth Submission"
      (is (= sub-60 (nth subs 59))))))

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


