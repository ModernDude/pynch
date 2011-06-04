(ns pynch.core
  (:require [net.cgrand.enlive-html :as enlv])
  (:require [clojure.string :as str])
  (:require [clj-time.core :as tm]))

(def *sub-url* "http://news.ycombinator.com/")
(def *crawl-delay* 30000)

(defrecord Submission [title url time points submitter cmnt-url cmnt-cnt])
(defrecord Comment [commenter time link texts])
(defrecord SubmissionDetails [submission notes comments])

(defn re-first-seq-digits
 "Uses regex to find the first sequential string of digits in
 the string given by s. If the string can not be found nil will
 be returned. The argument d can be used to indicate a default
 value in the case that a sequence of digits can not be found."
 ([s] (re-first-seq-digits s nil))
 ([s default]
    (let [found (re-find #"\d+" s)]
      (if (nil? found)
        default
        (Integer/parseInt found)))))

(defn now-nearest-minute []
  (let [dt (tm/now)
        remove-secs (fn [dt] (->> dt tm/sec tm/secs (tm/minus dt)))
        remove-milli (fn [dt] (->> dt tm/milli tm/millis (tm/minus dt)))]
    (-> dt remove-secs remove-milli)))

(defn ago-to-time [s]
 "Takes a string of the form 'x y ago' where x is an integral
 value and y indicates a period. For example, a string could
 be '3 days ago' or '5 hours ago'. This function will return
 a date-time object offset to reflect the date and time
 represented by the string."
 (let [offset (re-first-seq-digits s 0)
       periods ["minute", "hour", "day", "week", "month", "year"]
       period-found? (fn [p] (string? (re-find (re-pattern p) s)))
       first-found (fn [p] (if (period-found? p) p nil))
       create-period-fn (fn [p] (->> (str "clj-time.core/" p "s") symbol resolve))]
   (if-let [found-period (some first-found periods)]
     (tm/minus (now-nearest-minute) ((create-period-fn found-period)  offset))
     (now-nearest-minute))))

(def selectors
  {:sub-more-url [:td.title [:a (enlv/attr-starts :href "/x?fnid")]]
   :sub-ordinals [[:.title (enlv/has [(enlv/re-pred #"^[0-9]+\.{1}$")])]]
   :sub-titles [:.title :a]
   :sub-points [:td.subtext [:span (enlv/attr-starts :id "score_")]]
   :sub-users [:td.subtext [:a (enlv/attr-starts :href "user?")]]
   :sub-com-urls [:td.subtext [:a (enlv/attr-starts :href "item?")]]
   :sub-times [:td.subtext [enlv/text-node (enlv/text-pred #(re-find #"ago\s*\|" %))]]
   :notes [:table :tr :td :table [:tr (enlv/nth-child 4)]]
   :cmnt-users [:.default :.comhead [:a (enlv/attr-starts :href "user?")]]
   :cmnt-times [:.default :.comhead [enlv/text-node (enlv/text-pred #(re-find #"ago\s*\|" %))]]
   :cmnt-links [:.default :.comhead [:a (enlv/attr-starts :href "item?")]]
   :cmnt-text [[:.default (enlv/has [:a])]]
   :cmnt-text-paras #{[enlv/text-node]
                    
                      [:pre :> :code]  } })

(def extractors
  {:num (fn [node] (-> node enlv/text (re-first-seq-digits 0)))
   :url (fn [node] (-> node :attrs :href))
   :time (fn [node] (-> node ago-to-time))
   :text (fn [node] (-> node enlv/text))})

(defmulti get-res-uri class)
(defmethod get-res-uri :default [res] (java.net.URI. *sub-url*))
(defmethod get-res-uri java.net.URI [res] res)
(defmethod get-res-uri java.net.URL [res] (.toURI res))

(defn- get-next-page-uri [res more-link]
  "Find the next absolute uri based on the given uri and the
   more link found in ns."
   (let [new-res (get-res-uri res)]
    (if (= "file" (.getScheme new-res))
      (.resolve new-res (str/replace more-link #"^/" ""))
      (.resolve new-res more-link))))

(defn- select-more-url [ns]
  (->> :sub-more-url selectors (enlv/select ns) first ((extractors :url))))

(defn select-subs [ns]
  (first
   (enlv/let-select
    ns [titles (selectors :sub-titles)
        times (selectors :sub-times)
        points (selectors :sub-points)
        users (selectors :sub-users)
        comments (selectors :sub-com-urls)]
    (map #(Submission.
           ((extractors :text) %1)
           ((extractors :url) %1)
           ((extractors :time) %2)
           ((extractors :num) %3)
           ((extractors :text) %4)
           ((extractors :url) %5)
           ((extractors :num) %5))
         titles times points users comments))))

(defn- select-comment-paragraphs [ns]
  "Returns a sequece of strings representing each paragraph in the comment."
  (filter #(not= % "-----")
          (enlv/select ns (selectors :cmnt-text-paras))))

(defn- select-sub-comments [ns]
    ""
  (first
   (enlv/let-select
    ns [users (selectors :cmnt-users)
        times (selectors :cmnt-times)
        links (selectors :cmnt-links)
        cmnt-text (selectors :cmnt-text)]
    (map #(Comment. 
           ((extractors :text) %1)
           ((extractors :time) %2)
           ((extractors :url) %3)
           (select-comment-paragraphs %4))
         users times links cmnt-text))))

(defn- select-sub-notes [ns]
  (enlv/select ns [enlv/text-node]))

(defn- select-sub-details [ns]
  ""
  (first
   (enlv/let-select
    ns [titles (selectors :sub-titles)
        times (selectors :sub-times)
        points (selectors :sub-points)
        users (selectors :sub-users)
        notes (selectors :notes)
        comments (selectors :sub-com-urls)]
    (map #(SubmissionDetails.
           (Submission. 
            ((extractors :text) %1)
            ((extractors :url) %1)
            ((extractors :time) %2)
            ((extractors :num) %3)
            ((extractors :text) %4)
            ((extractors :url) %5)
            ((extractors :num) %5))
           (select-sub-notes %6)
           (select-sub-comments ns))
         titles times points users comments notes))))

(defn get-subs [res]
  "Returns a map of all submissions located at or within
   resource r. The type of x can be any of the following
   String, java.io.FileInputStream, java.io.Reader,
   java.io.InputStream, java.net.URL, java.net.URI"
 (-> res enlv/html-resource select-subs))

(defn get-subs-follow [res]
  "Loads the hacker news submission list given by the resource
   uri and returns a lazy list representing the parsed data."
 (lazy-seq
  (let [ns (enlv/html-resource res)
        more-url (select-more-url ns)
        next-uri (get-next-page-uri res more-url)]
    (concat (select-subs ns)
            (do (Thread/sleep *crawl-delay*)
                (get-subs-follow next-uri))))))

(defn get-sub-details [res]
  ""
  (-> res enlv/html-resource select-sub-details first))
