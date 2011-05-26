(ns pynch.test.core
  (:use [pynch.core])
  (:import [pynch.core Submission Comment SubmissionDetails])
  (:use [clojure.test])
  (:require [clj-time.core :as dt])
  (:use [clojure.java.io :as io]))

(defn static-now []
  "Function to return a static date that can be used for testing"
  (dt/date-time 2011 05 20))

(def sub-1
  (Submission.
   "Why Geeks Should Love HP WebOS"
   "http://developer.palm.com/blog/2011/05/10-reasons-for-geeks-to-love-hp-webos/"
   (dt/date-time 2011 05 19 21) 118 "unwiredben" "item?id=2538655" 49))

(def sub-14
  (Submission.
   "Google: Go ahead and hack the Chrome Book"
   "http://techcrunch.com/2011/05/11/hack-chromebooks"
   (dt/date-time 2011 05 19 18) 57 "MatthewB" "item?id=2537994" 0))

(def sub-30
  (Submission.
   "Finally, Google Tasks API"
   "https://code.google.com/apis/tasks/index.html"
   (dt/date-time 2011 05 19 18) 32 "toomanymike" "item?id=2538023" 4))

(def sub-31
  (Submission.
   "The False Choice Between Babies And Startups"
   "http://blogs.forbes.com/85broads/2011/05/16/the-false-choice-between-babies-and-startups/"
   (dt/date-time 2011 05 19 12) 46 "jemeshsu" "item?id=2554807" 30))

(def sub-60
  (Submission.
   "Exploring Lisp Libraries - and building a webapp on the way"
   "https://sites.google.com/site/sabraonthehill/home/exploring-quicklisp-packages"
   (dt/date-time 2011 05 19) 69 "mahmud" "item?id=2552163" 3))



(def sub-2498292
  (Submission.
   "Ask HN: Recommended Math Primer for SICP"
   "item?id=2498292"
   (dt/date-time 2011 04 28) 25 "imechura" "item?id=2498292" 12))

(def sub-2498292-text "Can you recommend a decent math primer for those of us who did not earn a degree in CS and would like to undertake the SICP text?")

(def cmnt-first
  (Comment.
   "swannodette" (dt/date-time 2011 04 28) "item?id=2498661" ["When I first read through SICP I stopped early in the book when it got the bit that required Calculus and took a massive detour to understand Calculus. Then I returned to the book, solved the problem, and discovered that the rest book had hardly any difficult math - certainly nothing that required me to know anything beyond high school math. C'est la vie."]))

(def cmnt-2
  (Comment.
   "orijing" (dt/date-time 2011 04 28) "item?id=2498466" ["I know this might not be so helpful for you, but I don't think it requires anything above a basic understanding of math. The \"deepest\" math was in the beginning, when it covered Newton's Method for square root approximation." "SICP is used as the introductory CS text at many universities (Berkeley included) and has no official math prerequisites. I think you should try reading it first, and if you get stuck on a concept like Newton's Method, you can just read about it on Wikipedia." "But otherwise, there was basically no math involved, except as simple illustrations. Good luck! It was a great text."]))

(def cmnt-last
  (Comment.
   "happy4crazy" (dt/date-time 2011 04 28) "item?id=2498480"
   ["Have you tried reading SICP without a math primer? What's your current math background?"]))


(deftest test-get-subs
  (binding [pynch.core/now static-now]
    (let [subs (-> "resources/submissions.html" io/resource get-subs)]
      (is (= 30 (count subs )) "Count Should Be 30")
      (is (= sub-1 (first subs)) "First Submission" )
      (is (= sub-14 (nth subs 13)) "Fourteenth Submission")
      (is (= sub-30 (last subs)) "Thirtieth Submission"))))

(deftest test-get-subs-follow
  (binding [pynch.core/*crawl-delay* 0
            pynch.core/now static-now]
    (let [subs (->> "resources/submissions.html" io/resource get-subs-follow (take 60))]
      (is (= 60 (count subs)) "Count should be 60")
      (is (= sub-1 (first subs)) "First Submission")
      (is (= sub-31 (nth subs 30))"Thirty-First Submission")
      (is (= sub-60 (nth subs 59)) "Sixtieth Submission"))))


(deftest test-get-sub-details
  (binding [pynch.core/now static-now]
    (let [details (-> "resources/item_2498292.html" io/resource get-sub-details)]
      (is (= sub-2498292 (:submission details)))
      (is (= sub-2498292-text (:paragraphs details)))
      (is (= 12 (count (:comments details))) "Should have 12 comments")
      (is (= cmnt-first (nth (:comments details) 0)))
      (is (= cmnt-2 (nth (:comments details) 1)))
      (is (= cmnt-last (last (:comments details))))))) 

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


