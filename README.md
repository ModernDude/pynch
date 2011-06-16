# Pynch

Pynch is a library for parsing submissions from news.arc sites
such as [Hacker News](http://news.ycombinator.com) and [Arc Forum](http://arclanguage.org/forum).  

## Usage

Get list of submissions on a given page

```clojure

(get-subs (java.net.URI. "http://news.ycombinator.com"))


```



## License

Copyright (C) 2011 Jeff Sigmon

Distributed under the Eclipse Public License, the same as Clojure.
