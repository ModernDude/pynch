(ns pynch.core
  (:use [net.cgrand.enlive-html :only
         [text has re-pred select attr-starts text-node
          text-pred html-resource texts let-select nth-child html-snippet]])
  (:require [clj-time.core :as tm])
  (:require [clojure.string :as str]))


(def *hn-url* "http://news.ycombinator.com/")
(def *crawl-delay* 0);30000)

(defn to-int [s]
 (Integer/parseInt s))

(def selectors
  {:sub-more-url [:td.title [:a (attr-starts :href "/x?fnid")]]
   :sub-ordinals [[:.title (has [(re-pred #"^[0-9]+\.{1}$")])]]
   :sub-titles [:.title :a]
   :sub-points [:td.subtext [:span (attr-starts :id "score_")]]
   :sub-users [:td.subtext [:a (attr-starts :href "user?")]]
   :sub-com-urls [:td.subtext [:a (attr-starts :href "item?")]]
   :sub-times [:td.subtext [text-node (text-pred #(re-find #"ago\s*\|" %))]]
   })

(defn- extract-num [node]
  (-> node text (re-first-seq-digits 0)))

(defn- extract-href [node]
  (-> node :attrs :href))

(defn- extract-time [node]
  (hn-time-to-dt node))


;(defn- select-href [ns sel-key]
;  (->> sel-key selectors (select ns) first :attrs :href)) 


(defn re-first-seq-digits
 "Uses regex to find the first sequential string of digits in
 the string given by s. If the string can not be found nil will
 be returned. The argument d can be used to indicate a default
 value in the case that a sequence of digits can not be found."
 ([s](re-first-seq-digits s nil)) ([s default]
    (let [found (re-find #"\d+" s)]
      (if (nil? found)
        default
        (to-int found)))))



(defn now []
  (let [d (tm/now)
        remove-secs (fn [d] (->> d tm/sec tm/secs (tm/minus d)))
        remove-milli (fn [d] (->> d tm/milli tm/millis (tm/minus d)))]
    (-> d remove-secs remove-milli)))
 



(defn hn-time-to-dt [s]
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
     (tm/minus (now) ((create-period-fn found-period)  offset))
     (now))))


(defmulti get-res-uri class)
(defmethod get-res-uri :default [res] (java.net.URI. *hn-url*))
(defmethod get-res-uri java.net.URI [res] res)
(defmethod get-res-uri java.net.URL [res] (.toURI res))
  



(defn- get-next-page-uri [res more-link]
  "Find the next absolute uri based on the given uri and the
   more link found in ns."
  (Thread/sleep *crawl-delay*)
  (let [new-res (get-res-uri res)]
    (if (= "file" (.getScheme new-res))
      (.resolve new-res (str/replace more-link #"^/" ""))
      (.resolve new-res more-link))))





(defn make-sub [ord title sub-url sub-time points
                user com-url com-cnt]
  {:ordinal ord
   :title title
   :submission-url sub-url
   :submission-time sub-time
   :points points
   :user user
   :comments-url com-url
   :comments-count com-cnt})


(defn- get-subs-map [ns]
  "Get a sequext of maps for each submission found in ns."
  (first
   (let-select
    ns [ordinals (:sub-ordinals selectors)
        titles (:sub-titles selectors)
        times (:sub-times selectors)
        points (:sub-points selectors)
        users (:sub-users selectors)
        comments (:sub-com-urls selectors)]
    (map #(make-sub
           (extract-num %1) (text %2) (extract-href %2) (extract-time %3)
           (extract-num %4) (text %5) (extract-href %6) (extract-num %6))
         ordinals titles times points users comments))))
                   
(defn get-subs [res]
  "Returns a map of all submissions located at or within
   resource r. The type of x can be any of the following
   String, java.io.FileInputStream, java.io.Reader,
   java.io.InputStream, java.net.URL, java.net.URI"
 (-> res html-resource get-subs-map))

(defn get-subs-follow [res]
  "Loads the hacker news submission list given by the resource
   uri and returns a lazy list representing the parsed data."
 (lazy-seq
  (let [ns (html-resource res)
        more-url (select-href ns :sub-more-url)
        next-uri (get-next-page-uri res more-url)]
    (concat
     (get-subs-map ns)
     (get-subs-follow next-uri)))))


(defn- get-comment-paragraphs [ns]
  "Returns a sequece of strings representing each paragraph in the comment."
 (->> (select ns #{[:p] [:font] }) texts (drop-last 2)))

(defn- get-submission-comments-dtl-map [ns]
  "Return a map of all submission comment details found in ns"
 (flatten
  (let-select
   ns
   [users [:.default :.comhead [:a (attr-starts :href "user?")]]
    com-times [:.default :.comhead
               [text-node (text-pred #(re-find #"ago\s*|" %))]]
    com-links [:.default :.comhead [:a (attr-starts :href "item?")]]
    comments [:.default]; (html/has [:*])]]
    ]

    (map (fn [user com-time com-link comment]
          {:user (text user)
           :comment-time (hn-time-to-dt com-time)
           :comment-link (-> com-link :attrs :href)
           :comment (get-comment-paragraphs comment)
           })
         users com-times com-links comments))))



(defn- get-submission-comments-hdr-map [ns]
  "Return a map containing all submission comments found in ns."
 (let-select
  ns
  [titles [:td.title]
   points [:td.subtext [:span (attr-starts :id "score_")]]
   users [:td.subtext [:a (attr-starts :href "user?")]]
   com-links [:td.subtext [:a (attr-starts :href "item?")]]
   sub-times [:td.subtext
              [text-node (text-pred #(re-find #"ago\s*\|" %))]]
   notes [:table :tr :td :table [:tr (nth-child 4)]]]
  (map (fn [title point user com-link sub-time note]
   {:title (text title)
    :submission-time (hn-time-to-dt sub-time)
    :points (-> points text (re-first-seq-digits 0))
    :user (text user)
    :comments-count (-> com-links text (re-first-seq-digits 0))
    :notes (text note)
    :comments (get-submission-comments-dtl-map ns)})
       titles points users com-links sub-times notes)))


(defmulti get-comments class)

(defmethod get-comments String [s]
  "The string containing html provided by s will be parsed
   and a map will be returned containing all details relating 
   to discussions on hacker news for the specified submission."
 (-> s html-snippet get-submission-comments-hdr-map))

(defmethod get-comments java.net.URI [uri]
  "The resource provided by uri will be loaded and parsed 
   and a map will be returned containing all details relating
   to discussions on hacker news for the specified submission."
 (-> uri html-resource get-submission-comments-hdr-map first))
