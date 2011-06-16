(defproject pynch "0.1.0-alpha"
  :description "Pynch is a library for parsing submissions from news.arc sites such as Hacker News and Arc Forum."
  :url "https://github.com/jeffsigmon/pynch"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [enlive "1.0.0"]
                 [clj-time "0.3.0"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [lein-clojars "0.6.0"]])
