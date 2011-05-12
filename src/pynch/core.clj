(ns pynch.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clj-time.core :as tm]))


(def *hn-url* "http://news.ycombinator.com/")
(def *crawl-delay* 30000)

(defn to-int [s]
 (Integer/parseInt s))


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
       create-period-fn (fn [p] (->> (str "clj-time.core/" p "s")
symbol resolve))]
   (if-let [found-period (some first-found periods)]
     (tm/minus (tm/now) ((create-period-fn found-period)  offset))
     (tm/now))))


(defn- get-next-page-uri [ns uri]
  "Find the next absolute uri based on the given uri and the
   more link found in ns."
 (Thread/sleep *crawl-delay*)
 (let [node (html/select ns [:td.title
                             [:a (html/attr-starts :href "/x?fnid")]])
       link (-> node first :attrs :href)]
   (.resolve uri link)))

(defn- get-submissions-map [ns]
  "Get a sequext of maps for each submission found in ns."
 (html/let-select
  ns
  [ordinals [[:.title (html/has [(html/re-pred #"^[0-9]+\.{1}$")])]]
   titles [:.title :a]
   points [:td.subtext [ :span (html/attr-starts :id "score_")]]
   users [:td.subtext [ :a (html/attr-starts :href "user?")]]
   com-links [:td.subtext [ :a (html/attr-starts :href "item?")]]
   sub-times [:td.subtext
              [html/text-node (html/text-pred #(re-find #"ago\s*\|" %)) ]]]
  (map (fn [ordinal title point user com-link sub-time]
   {:ordinal (-> ordinal html/text (re-first-seq-digits 0))
    :title (html/text title)
    :submission-url (-> title :attrs :href)
    :submission-time (hn-time-to-dt sub-time)
    :points (-> points html/text (re-first-seq-digits 0))
    :user (html/text user)
    :comments-url (-> com-link :attrs :href)
    :comments-count (-> com-link html/text (re-first-seq-digits 0) )})
       ordinals titles points users com-links sub-times)))


(defmulti get-submissions class)

(defmethod get-submissions String [s]
  "Loads the html string s represnting a hacker news list
   of articles and returns  sequence of mapped data."
 (-> s html/html-snippet get-submissions-map))

(defmethod get-submissions java.net.URI [uri]
  "Loads the hacker news submission list given by the resource
   uri and returns a lazy list representing the parsed data."
 (lazy-seq
  (let [ns (html/html-resource uri)
        next-uri (get-next-page-uri ns uri)]
    (concat
     (flatten
      (get-submissions-map ns))
     (get-submissions next-uri)))))


(defn- get-comment-paragraphs [ns]
  "Returns a sequece of strings representing each paragraph in the comment."
 (->> (html/select ns #{[:p] [:font] }) html/texts (drop-last 2)))

(defn- get-submission-comments-dtl-map [ns]
  "Return a map of all submission comment details found in ns"
 (flatten
  (html/let-select
   ns
   [users [:.default :.comhead [:a (html/attr-starts :href "user?")]]
    com-times [:.default :.comhead
               [html/text-node (html/text-pred #(re-find #"ago\s*|" %))]]
    com-links [:.default :.comhead [:a (html/attr-starts :href "item?")]]
    comments [:.default]; (html/has [:*])]]
    ]

    (map (fn [user com-time com-link comment]
          {:user (html/text user)
           :comment-time (hn-time-to-dt com-time)
           :comment-link (-> com-link :attrs :href)
           :comment (get-comment-paragraphs comment)
           })
         users com-times com-links comments))))



(defn- get-submission-comments-hdr-map [ns]
  "Return a map containing all submission comments found in ns."
 (html/let-select
  ns
  [titles [:td.title]
   points [:td.subtext [:span (html/attr-starts :id "score_")]]
   users [:td.subtext [:a (html/attr-starts :href "user?")]]
   com-links [:td.subtext [:a (html/attr-starts :href "item?")]]
   sub-times [:td.subtext
              [html/text-node (html/text-pred #(re-find #"ago\s*\|" %))]]
   notes [:table :tr :td :table [:tr (html/nth-child 4)]]]
  (map (fn [title point user com-link sub-time note]
   {:title (html/text title)
    :submission-time (hn-time-to-dt sub-time)
    :points (-> points html/text (re-first-seq-digits 0))
    :user (html/text user)
    :comments-count (-> com-links html/text (re-first-seq-digits 0))
    :notes (html/text note)
    :comments (get-submission-comments-dtl-map ns)})
       titles points users com-links sub-times notes)))


(defmulti get-comments class)

(defmethod get-comments String [s]
  "The string containing html provided by s will be parsed
   and a map will be returned containing all details relating 
   to discussions on hacker news for the specified submission."
 (-> s html/html-snippet get-submission-comments-hdr-map))

(defmethod get-comments java.net.URI [uri]
  "The resource provided by uri will be loaded and parsed 
   and a map will be returned containing all details relating
   to discussions on hacker news for the specified submission."
 (-> uri html/html-resource get-submission-comments-hdr-map first))
