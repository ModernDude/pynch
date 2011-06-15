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
  {:title "Why Geeks Should Love HP WebOS"
   :points 118
   :sub-time (dt/date-time 2011 05 19 21)
   :sub-url "http://developer.palm.com/blog/2011/05/10-reasons-for-geeks-to-love-hp-webos/"
   :user "unwiredben"
   :com-url "item?id=2538655"
   :com-count 49})

(def sub-14
  {:title "Google: Go ahead and hack the Chrome Book"
   :points 57
   :sub-time (dt/date-time 2011 05 19 18)
   :sub-url "http://techcrunch.com/2011/05/11/hack-chromebooks"
   :user "MatthewB"
   :com-url "item?id=2537994"
   :com-count 0})

(def sub-30
  {:title "Finally, Google Tasks API"
   :points 32
   :sub-time (dt/date-time 2011 05 19 18)
   :sub-url "https://code.google.com/apis/tasks/index.html"
   :user "toomanymike"
   :com-url "item?id=2538023"
   :com-count 4})

(def sub-31
  {:title "The False Choice Between Babies And Startups"
   :points 46
   :sub-time (dt/date-time 2011 05 19 12)
   :sub-url "http://blogs.forbes.com/85broads/2011/05/16/the-false-choice-between-babies-and-startups/"
   :user "jemeshsu"
   :com-url "item?id=2554807"
   :com-count 30})

(def sub-60
  {:title "Exploring Lisp Libraries - and building a webapp on the way"
   :points 69
   :sub-time (dt/date-time 2011 05 19)
   :sub-url "https://sites.google.com/site/sabraonthehill/home/exploring-quicklisp-packages"
   :user "mahmud"
   :com-url "item?id=2552163"
   :com-count 3})

(def testdetail
  {:title "Ask HN: Recommended Math Primer for SICP"
   :time (dt/date-time 2011 04 24)
   :points 25
   :user "imechura"
   :notes "Can you recommend a decent math primer for those of us who did not earn a degree in CS and would like to undertake the SICP text?"
   :com-url "item?id=2498292"
   :com-count 12})

(def detail-cmnt-1
  {:cmnt-text ["When I first read through SICP I stopped early in the book when it got the bit that required Calculus and took a massive detour to understand Calculus. Then I returned to the book, solved the problem, and discovered that the rest book had hardly any difficult math - certainly nothing that required me to know anything beyond high school math. C'est la vie."]
   :cmnt-url "item?id=2498661"
   :user "swannodette"
   :time (dt/date-time 2011 04 24)})

(def detail-cmnt-2
  {:cmnt-text ["I know this might not be so helpful for you, but I don't think it requires anything above a basic understanding of math. The \"deepest\" math was in the beginning, when it covered Newton's Method for square root approximation."
     "SICP is used as the introductory CS text at many universities (Berkeley included) and has no official math prerequisites. I think you should try reading it first, and if you get stuck on a concept like Newton's Method, you can just read about it on Wikipedia."
     "But otherwise, there was basically no math involved, except as simple illustrations. Good luck! It was a great text."]
    :cmnt-url "item?id=2498466",
    :user "orijing",
    :time (dt/date-time 2011 04 24)}) 

(def detail-cmnt-last
  {:cmnt-text ["The SICP, from the chapters I've read, does not rely on  mathematical sophistication the way, say, Knuth does. There's a little bit of calculus, but not much. If you've had a semester of calculus, you're probably more than well prepared. If you haven't you can probably focus instead on the data structures and algorithms." "Your best bet is just to grab a Scheme interpreter and dive in."]
   :user "happy4crazy"
   :time (dt/date-time 2011 04 24)
   :cmnt-url "item?id=2498480"})

(deftest test-get-subs-default
  (binding [pynch.core/now-nearest-minute static-now]
    (let [subs (-> "resources/submissions.html" io/resource get-subs)]
      (is (= 30 (count subs )) "Count Should Be 30")
      (is (= sub-1 (first subs)) "First Submission" )
      (is (= sub-14 (nth subs 13)) "Fourteenth Submission")
      (is (= sub-30 (last subs)) "Thirtieth Submission"))))

(deftest test-get-subs-default-follow
  (binding [pynch.core/*crawl-delay* 0
            pynch.core/now-nearest-minute static-now]
    (let [subs (->> "resources/submissions.html" io/resource get-subs-follow (take 60))]
      (is (= 60 (count subs)) "Count should be 60")
      (is (= sub-1 (first subs)) "First Submission")
      (is (= sub-31 (nth subs 30))"Thirty-First Submission")
      (is (= sub-60 (nth subs 59)) "Sixtieth Submission"))))

;(deftest test-get-subs-fields-supplied
;  (binding [pynch.core/now-nearest-minute static-now]
;    (let [fields (filter #( ) pynch.core/sub-fields)
;          subs (-> "resources/submissions/html" io/resource (get-subs (pynch/sub-fields

(deftest test-get-sub-details
  (binding [pynch.core/now-nearest-minute static-now]
    (let [[detail & _] (-> "resources/item_2498292.html" io/resource get-sub-details)]
      (testing "Detail Headers"
        (is (= (:title testdetail) (:title detail)))
        (is (= (:user testdetail) (:user detail)))
        (is (= (:time testdetail) (:time detail)))
        (is (= (:points testdetail) (:points detail)))
        (is (= (:notes testdetail) (:notes detail)))
        (is (= (:com-url testdetail) (:com-url detail)))
        (is (= 12 (count (:comments detail))) "Should have 12 comments"))
      (testing "Test First Comment"
        (let [cmnt (first (:comments detail))]
          (is (= (:time cmnt) (:time detail-cmnt-1)))
          (is (= (:user cmnt) (:user detail-cmnt-1)))
          (is (= (:cmnt-url cmnt) (:cmnt-url detail-cmnt-1)))
          (is (= (:cmnt-text cmnt) (:cmnt-text detail-cmnt-1)))))
      (testing "Test Second Comment"
        (let [cmnt (second (:comments detail))]
          (is (= (:time cmnt) (:time detail-cmnt-2)))
          (is (= (:user cmnt) (:user detail-cmnt-2)))
          (is (= (:cmnt-url cmnt) (:cmnt-url detail-cmnt-2)))
          (is (= (:cmnt-text cmnt) (:cmnt-text detail-cmnt-2)))))
      (testing "Test Last Comment"
        (let [cmnt (last (:comments detail))]
          (is (= (:time cmnt) (:time detail-cmnt-last)))
          (is (= (:user cmnt) (:user detail-cmnt-last)))
          (is (= (:cmnt-url cmnt) (:cmnt-url detail-cmnt-last)))
          (is (= (:cmnt-text cmnt) (:cmnt-text detail-cmnt-last))))))))

      
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

(deftest test-ago-to-time
  (testing "Time Period Specificed"
    (testing "Years"
      (is (dates-same? ((now-minus) (dt/years 10)) (ago-to-time "10 years ago")))
      (is (dates-same? ((now-minus) (dt/years 10)) (ago-to-time "10 year ago"))))
    (testing "Months"
      (is (dates-same? ((now-minus) (dt/months 4)) (ago-to-time "4 months ago")))
      (is (dates-same? ((now-minus) (dt/months 4)) (ago-to-time "4 months ago"))))
    (testing "Weeks"
      (is (dates-same? ((now-minus) (dt/weeks 4)) (ago-to-time "4 weeks ago")))
      (is (dates-same? ((now-minus) (dt/weeks 4)) (ago-to-time "4 week ago"))))
     (testing "Days"
      (is (dates-same? ((now-minus) (dt/days 4)) (ago-to-time "4 days ago")))
      (is (dates-same? ((now-minus) (dt/days 4)) (ago-to-time "4 day ago"))))
     (testing "Hours"
      (is (dates-same? ((now-minus) (dt/hours 4)) (ago-to-time "4 hours ago")))
      (is (dates-same? ((now-minus) (dt/hours 4)) (ago-to-time "4 hour ago"))))
     (testing "Minutes"
      (is (dates-same? ((now-minus) (dt/minutes 4)) (ago-to-time "4 minutes ago")))
      (is (dates-same? ((now-minus) (dt/minutes 4)) (ago-to-time "4 minute ago")))))
  (testing "Time Period Not Specified"
    (is (dates-same? (dt/now) (ago-to-time "")))))


