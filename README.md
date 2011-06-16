# Pynch

Pynch is a library for parsing submissions from news.arc sites
such as [Hacker News](http://news.ycombinator.com) and [Arc Forum](http://arclanguage.org/forum).  

## Quick Start - Emacs/Lein/Swank

1. Create leiningen project

    lein new testpynch

2. Update testpynch/project.clj

```clojure

(defproject testpynch "1.0.0-SNAPSHOT"
  :description "Test Pynch"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [pynch "0.1.0-alpha"]]
  :dev-dependencies [[swank-clojure "1.2.1"]])

```

3. Update testpynch/src/testpynch/core.clj

```clojure

(ns ptest.core
  (require [pynch.core :as py]))

(defn get-first-hn-sub []
  (-> "http://news.ycombinator.com" java.net.URI. py/get-subs first))

```

4. Use lein to grab dependencies

    lein deps

5. Start swank server

    lein swank

6. Open core.clj in emacs and connect with slime

    M-x slime-connect

7. Build

    C-c k

8. At REPL

    user> (ns pynchtest.core)

    pynchtest.core> (get-first-hn-sub)


## API

### get-subs

Usage: 

```clojure
(get-subs res)
(get-subs res fields)
```

Returns a sequence of maps for each submission located at or within
res. The type of res can be any of the following (String,
java.io.FileInputStream, java.io.Reader, java.io.InputStream,
java.net.URL, java.net.URI). The param fields is optional and can be
used to specify a coll of fields that will be selected, extracted and
returned from function call. Each field must implement the FieldSpec
protocol. If fields is not supplied, a default list of fields
specified by *default-sub-fields* will be' used. (get-subs
(java.net.URI. "http://news.ycombinator.com"))


### get-subs-crawl

Usage: 

```clojure  
(get-subs-crawl res)
(get-subs-crawl res fields)
```

Returns a lazy seqence of maps for each submission located at or
within res followed by the submissions on the next page and so on. The
function will return submissions as long as it can find a 'more' page
to grab. The function will sleep for *crawl-delay* seconds in between
each request. Just as with the get-subsfunction, an optional fields
collection can specified to define what data get is returned.

**Warning**

Calling this function will most likely violate the robots.txt file on
the target web server so I would recommend getting permission from the
target proprietor before doing anything serious with this.

### get-sub-details

Usage:

```clojure
(get-sub-details res)
(get-sub-details res fields)
```

Returns details, including comments, for the submission located at or
within res. The type of res can be any of the following (String,
java.io.FileInputStream, java.io.Reader, java.io.InputStream,
java.net.URL, java.net.URI). The param fields is optional and can be
used to specify a coll of fields that will be selected, extracted and
returned from function call. Each field must implement the FieldSpec
protocol. If fields is not supplied, a default list of fields
specified by *default-detail-fields* will be' used.


## Available Fields

TODO


## Extending

TODO


## Known Issues

1. If using swank-clojure you must use v1.2.1 or less. Please
reference https://github.com/technomancy/swank-clojure/issues/32

2. If a submissions list source html is missing user, time, or comment
count the output will not be correct. This is because each field
selecter is run independently and merged together. The solution assumes that 
each submission provides each piece of information. There are rare
times when some information is not provided, in these cases the other
fields will be mismatched.

## License

Copyright (C) 2011 Jeff Sigmon

Distributed under the Eclipse Public License, the same as Clojure.
