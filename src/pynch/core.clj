(ns pynch.core
  (:use [net.cgrand.enlive-html :only
         [text has re-pred select attr-starts text-node but
          text-pred html-resource texts let-select nth-child html-snippet]])
  (:require [clj-time.core :as tm])
  (:require [clojure.string :as str]))


(def *hn-url* "http://news.ycombinator.com/")
(def *crawl-delay* 30000)


(defrecord subm [title url subm-time points user cmnt-url cmnt-cnt])
(defrecord cmnt [user time link paragraphs])
(defrecord subm-dtls [subm paragraphs comments])



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

(def selectors
  {:sub-more-url [:td.title [:a (attr-starts :href "/x?fnid")]]
   :sub-ordinals [[:.title (has [(re-pred #"^[0-9]+\.{1}$")])]]
   :sub-titles [:.title :a]
   :sub-points [:td.subtext [:span (attr-starts :id "score_")]]
   :sub-users [:td.subtext [:a (attr-starts :href "user?")]]
   :sub-com-urls [:td.subtext [:a (attr-starts :href "item?")]]
   :sub-times [:td.subtext [text-node (text-pred #(re-find #"ago\s*\|" %))]]
   :notes [:table :tr :td :table [:tr (nth-child 4)]]
   :cmnt-users [:.default :.comhead [:a (attr-starts :href "user?")]]
   :cmnt-times [:.default :.comhead [text-node (text-pred #(re-find #"ago\s*\|" %))]]
   :cmnt-links [:.default :.comhead [:a (attr-starts :href "item?")]]
   :cmnt-text [[:.default (has [:a])  ] ]
   :cmnt-text-paras #{[:font :> text-node]
                      [:p :> text-node] }
   })

(defn- extract-num [node]
  (-> node text (re-first-seq-digits 0)))

(defn- extract-href [node]
  (-> node :attrs :href))

(defn- extract-time [node]
  (hn-time-to-dt node))

(defn- extract-paragraphs [ns]
  "Returns a sequece of strings representing each paragraph in the comment."
  (drop-last
    (select ns (:cmnt-text-paras selectors))))


(defmulti get-res-uri class)
(defmethod get-res-uri :default [res] (java.net.URI. *hn-url*))
(defmethod get-res-uri java.net.URI [res] res)
(defmethod get-res-uri java.net.URL [res] (.toURI res))
  
(defn- get-more-url [ns]
  (-> (select ns (:sub-more-url selectors)) first extract-href))

(defn- get-next-page-uri [res more-link]
  "Find the next absolute uri based on the given uri and the
   more link found in ns."
 
  (let [new-res (get-res-uri res)]
    (if (= "file" (.getScheme new-res))
      (.resolve new-res (str/replace more-link #"^/" ""))
      (.resolve new-res more-link))))

(defn get-subs-rec [ns]
  (first
   (let-select
    ns [ordinals (:sub-ordinals selectors)
        titles (:sub-titles selectors)
        times (:sub-times selectors)
        points (:sub-points selectors)
        users (:sub-users selectors)
        comments (:sub-com-urls selectors)]
    (map #(subm. (text %1) (extract-href %1) (extract-time %2)
           (extract-num %3) (text %4) (extract-href %5) (extract-num %5))
         titles times points users comments))))
                   
(defn get-subs [res]
  "Returns a map of all submissions located at or within
   resource r. The type of x can be any of the following
   String, java.io.FileInputStream, java.io.Reader,
   java.io.InputStream, java.net.URL, java.net.URI"
 (-> res html-resource get-subs-rec))

(defn get-subs-follow [res]
  "Loads the hacker news submission list given by the resource
   uri and returns a lazy list representing the parsed data."
 (lazy-seq
  (let [ns (html-resource res)
        more-url (get-more-url ns)
        next-uri (get-next-page-uri res more-url)]
    (concat (get-subs-rec ns)
            (do (Thread/sleep *crawl-delay*)
                (get-subs-follow next-uri))))))


(defn make-item-comment [user time link cmnt-paras]
  {:user user
   :time time
   :link link
   :cmnt-paras cmnt-paras})

(defn get-item-comments-map [ns]
  ""
  (first
   (let-select
    ns [users (:cmnt-users selectors)
        times (:cmnt-times selectors)
        links (:cmnt-links selectors)
        cmnt-text (:cmnt-text selectors)]
    (map #(make-item-comment
           (text %1)
           (extract-time %2)
           (extract-href %3)
           (extract-paragraphs %4))
         
         users times links cmnt-text))))
 


(defn make-item [title sub-time points
                 user notes comments]
  {:title title
   :submission-time sub-time
   :points points
   :user user
   :notes notes
   :comments comments})

(defn get-item-map [ns]
  ""
  (first
   (let-select
    ns [titles (:sub-titles selectors)
        times (:sub-times selectors)
        points (:sub-points selectors)
        users (:sub-users selectors)
        notes (:notes selectors)]
    (map #(make-item
           (text %1)
           (extract-time %2)
           (extract-num %3)
           (text %4)
           (text %5)
           (get-item-comments-map ns))
         titles times points users notes))))

(defn get-item [res]
  ""
  (-> res html-resource get-item-map first))


