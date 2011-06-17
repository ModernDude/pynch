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
returned from the function call. Each field must implement the FieldSpec
protocol. If fields is not supplied, a default list of fields
specified by \*default-sub-fields\* will be' used. (get-subs
(java.net.URI. "http://news.ycombinator.com"))


### get-subs-crawl

Usage: 

```clojure  
(get-subs-crawl res)
(get-subs-crawl res fields)
```

Returns a lazy sequence of maps for each submission located at or
within res followed by the submissions on the next page and so on. The
function will return submissions as long as it can find a 'more' page
to grab. The function will sleep for \*crawl-delay\* seconds in between
each request. Just as with the get-subs function, an optional fields
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
specified by \*default-detail-fields\* will be' used.


## Available Fields

The following table describes the fields that come baked into
pynch. The fields bound to the symbol **sub-fields** should be used
with the **get-subs\*** functions. All other fields should be used
with the **get-sub-details** function.

<table>
<tr><td>Binding</td><td>Key</td><td>Type</td><td>Default</td></tr>
<tr><td>sub-fields</td><td>:ordinal</td><td>int</td><td>false</td></tr>
<tr><td>sub-fields</td><td>:points</td><td>int</td><td>true</td></tr>
<tr><td>sub-fields</td><td>:sub-time</td><td>Date</td><td>true</td></tr>
<tr><td>sub-fields</td><td>:sub-url</td><td>string</td><td>true</td></tr>
<tr><td>sub-fields</td><td>:user</td><td>string</td><td>true</td></tr>
<tr><td>sub-fields</td><td>:com-url</td><td>string</td><td>true</td></tr>
<tr><td>sub-fields</td><td>:com-count</td><td>int</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:title</td><td>string</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:time</td><td>Date</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:points</td><td>int</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:notes</td><td>string</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:com-url</td><td>string</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:com-count</td><td>int</td><td>true</td></tr>
<tr><td>detail-fields</td><td>:comments</td><td>List of
comment-fields</td><td>true</td></tr>
<tr><td>comment-fields</td><td>:user</td><td>string</td><td>true</td></tr>
<tr><td>comment-fields</td><td>:time</td><td>date</td><td>true</td></tr>
<tr><td>comment-fields</td><td>:cmnt-url</td><td>string</td><td>true</td></tr>
<tr><td>comment-fields</td><td>:cmnt-text</td><td>List of
strings</td><td>true</td></tr>
<tr><td>comment-fields</td><td>:cmnt-nodes</td><td>List of html nodes</td><td>false</td></tr>
</table>

If no fields are specified in a function call, the default fields
will be returned as defined above. 

There are currently two ways to change the fields that are returned
from a function. 

1. Rebind the default fields before calling a function.

You can do this by rebinding any of the following symbols:

* \*default-sub-fields\*
* \*default-detail-fields\*
* \*default-comment-fields\*

Each symbol is bound to the appropriate list of keys from the table
above. As an example, If I only wanted the points from submissions
returned, I could do the following:

```clojure

(binding [py/*default-sub-fields* [:points]]
   (py/get-subs (java.net.URI. "http://news.ycombinator.com")))

```

If you want to change the default comment fields returned on the
**get-sub-details** call, this is the easiest method because the
function only accepts detail fields and not comment fields.

2. Pass in a list of fields that you want to select

```clojure

(py/get-subs (java.net.URI. "http://news.ycombinator.com"))
(py/get-field-specs [:points] py/sub-fields)

```

## Extending

You can add in your own field definitions to be returned if you
desire. You can even use this library to parse sequences over non
news.arc sites although I'm not sure if anyone would find much use in
that (see the *select-fields* function). Regardless, if you want to
add-in a field or two you simply need to pass in an object into the
fields collection that implements the following protocol:

```clojure

(defprotocol FieldSpecifier
  "Describes how a field can identify, select and extract
   iteself from an html document."
  (get-selector [_])
  (extract-field [_ node])
  (get-key [_]))


```

* get-selector must return a css selector as specified in the Enlive
  project https://github.com/cgrand/enlive
* extract-field must extract the desired value from the selected dom
  node
* get-key is how the field will identify itself in the returned map.



## Known Issues

1. If using swank-clojure you must use v1.2.1 or less. Please
reference https://github.com/technomancy/swank-clojure/issues/32

2. If a submissions list source html is missing user, time, or comment
count the output will not be correct. This is because each field
selector is run independently and merged together. The solution assumes that 
each submission provides each piece of information. There are rare
times when some information is not provided, in these cases the other
fields will be mismatched.

## License

Copyright (C) 2011 Jeff Sigmon

Distributed under the Eclipse Public License, the same as Clojure.
