(ns pynch.core
  (:require [net.cgrand.enlive-html :as enlv])
  (:require [clojure.string :as str])
  (:require [clj-time.core :as tm]))

(def *sub-url* "http://news.ycombinator.com/")
(def *crawl-delay* 30000)

(def *default-sub-fields* [:points :title :sub-time :sub-url :user :com-url :com-count ])
(def *default-detail-fields* [:title :time :points :user :notes :com-url :com-count :comments])
(def *default-comment-fields* [:user :time :cmnt-url :cmnt-text])
  
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

(declare selectors extractors)

(defprotocol FieldSpecifier
  (get-selector [_])
  (extract-field [_ node])
  (get-key [_]))

(defrecord FieldSpec [key selector-key extractor-key]
  FieldSpecifier
  (get-selector [_] (selectors selector-key))
  (extract-field [_ node] ((extractors extractor-key) node))
  (get-key [_] key))

(defn- get-field-specs [keyseq coll]
  "Take a seq of of field keys and return a collection of the matching
FieldSpec records"
  (letfn [(keys-match? [key field] (= (get-key field) key))
          (key-to-field [key] (filter (partial keys-match? key) coll))]
    (flatten (map key-to-field keyseq))))

(defn select-fields [ns fields]
  (letfn [(select-nodes [field]
            (enlv/select ns (get-selector field)))
          (get-field-extract-map-coll [nodes]
            (reverse
             (map (fn [field node]
                    (hash-map (get-key field) (extract-field field node)))
                  fields nodes)))
          (nodes-to-map [& nodes]
            (reduce #(merge %1 %2) {} (get-field-extract-map-coll nodes)))]
    (apply map nodes-to-map (map select-nodes fields))))

(def sub-fields
  [(FieldSpec. :ordinal :sub-ordinals :num)
   (FieldSpec. :points :sub-points :num)
   (FieldSpec. :title :sub-titles :text)
   (FieldSpec. :sub-time :sub-times :time)
   (FieldSpec. :sub-url :sub-titles :url)
   (FieldSpec. :user :sub-users :text)
   (FieldSpec. :com-url :sub-com-urls :url)
   (FieldSpec. :com-count :sub-com-urls :num)])

(def detail-fields
  [(FieldSpec. :title :sub-titles :text)
   (FieldSpec. :time :sub-times :time)
   (FieldSpec. :points :sub-points :num)
   (FieldSpec. :user :sub-users :text)
   (FieldSpec. :notes :notes :text)
   (FieldSpec. :com-url :sub-com-urls :url)
   (FieldSpec. :com-count :sub-com-urls :num)
   (FieldSpec. :comments :comments :comments)])

(def comment-fields
  [(FieldSpec. :user :cmnt-users :text)
   (FieldSpec. :time :cmnt-times :time)
   (FieldSpec. :cmnt-url :cmnt-links :url)
   (FieldSpec. :cmnt-text :cmnt-text-paras :comment)
   (FieldSpec. :cmnt-nodes :cmnt-text-paras :identity)])

(def selectors
  {:sub-more-url [:td.title [:a (enlv/attr-starts :href "/x?fnid")]]
   :sub-ordinals [[:.title (enlv/has [(enlv/re-pred #"^[0-9]+\.{1}$")])]]
   :sub-titles [:.title :a]
   :sub-points [:td.subtext [:span (enlv/attr-starts :id "score_")]]
   :sub-users [:td.subtext [:a (enlv/attr-starts :href "user?")]]
   :sub-com-urls [:td.subtext [:a (enlv/attr-starts :href "item?")]]
   :comments [:body]
   :sub-times [:td.subtext [enlv/text-node (enlv/text-pred #(re-find #"ago\s*\|" %))]]
   :notes [:table :tr :td :table [:tr (enlv/nth-child 4)]]
   :cmnt-users [:.default :.comhead [:a (enlv/attr-starts :href "user?")]]
   :cmnt-times [:.default :.comhead [enlv/text-node (enlv/text-pred #(re-find #"ago\s*\|" %))]]
   :cmnt-links [:.default :.comhead [:a (enlv/attr-starts :href "item?")]]
   :cmnt-text [[:.default (enlv/has [:a])]]
   :cmnt-text-paras  [:.default]
   :cmnt-text-text [:* [enlv/text-node]]})

(def extractors
  {:num (fn [node] (-> node enlv/text (re-first-seq-digits 0)))
   :url (fn [node] (-> node :attrs :href))
   :time (fn [node] (-> node ago-to-time))
   :text (fn [node] (-> node enlv/text))
   :identity (fn [node] node)
   :comments (fn [node] (select-fields node (get-field-specs *default-comment-fields* comment-fields)))
   ;;;This is so complicated because the hn comment html is pretty fucked up and
   ;;;looks like it has parsing issues coming through sax.
   :comment (fn [node]
               (letfn [(comhead? [n] (= "comhead" (-> n :attrs :class)))
                       (anchor? [n] (= :a (:tag n)))
                       (href [n] (-> n :attrs :href))
                       (content-coll? [n] (-> n :content coll?))
                       (reply? [n] (and (anchor? n) (re-find #"reply?" (href n))))
                       (node-branch? [n] (and (not (comhead? n))
                                              (not (reply? n))
                                              (content-coll? n)))
                       (filter-pred [n] (and (string? n) (not= "\n" n) (not= "-----" n)))]
                 (filter filter-pred (tree-seq node-branch? :content node))))})

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

(defn get-subs
  "Returns a map of all submissions located at or within
   resource r. The type of x can be any of the following
   String, java.io.FileInputStream, java.io.Reader,
   java.io.InputStream, java.net.URL, java.net.URI"
  ([res]
     (get-subs res (get-field-specs *default-sub-fields* sub-fields)))
  ([res fields]
     (select-fields (enlv/html-resource res) fields)))

(defn get-subs-follow
  "Loads the hacker news submission list given by the resource
   uri and returns a lazy list representing the parsed data."
  ([res]
     (get-subs-follow res (get-field-specs *default-sub-fields* sub-fields)))
  ([res fields]
     (lazy-seq
      (let [ns (enlv/html-resource res)
            more-url (select-more-url ns)
            next-uri (get-next-page-uri res more-url)]
        (concat (get-subs ns fields)
            (do (Thread/sleep *crawl-delay*)
                (get-subs-follow next-uri fields)))))))

(defn get-sub-details
  ""
  ([res]
     (get-sub-details res (get-field-specs *default-detail-fields* detail-fields)))
  ([res fields]
     (select-fields (enlv/html-resource res) fields)))     


