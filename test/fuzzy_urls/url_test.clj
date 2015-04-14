(ns fuzzy-urls.url-test
  (:require [expectations :refer :all]
            [fuzzy-urls.url :refer :all]))

; some basic unit tests

(expect
  {:scheme "http", :user "sky", :host "www", :port 801,
   :path ["cgi-bin" "finger"],
   :query {:name "shriram", :host "nw"},
   :fragment "top"}
  (string->url "http://sky@www:801/cgi-bin/finger?name=shriram&host=nw#top"))

(expect
  {:scheme "foo", :user nil, :host nil, :port nil, :path [], :query {}, :fragment nil}
  (string->url "foo://"))

(expect
  {:scheme nil, :user nil, :host "bar", :port nil, :path [], :query {}, :fragment nil}
  (string->url "//bar"))
(expect
  {:scheme nil, :user nil, :host nil, :port nil, :path [], :query {}, :fragment nil}
  (string->url "bar"))

; test empty vs nil query parameters
(expect
  {:query {:a "b" :c "" :d nil}}
  (in (string->url "?a=b&c=&d")))

; test case normalizing
(expect {:scheme "http"} (in (string->url "HtTp:")))
(expect {:host "example.com"} (in (string->url "//eXaMpLE.CoM")))
(expect {:user "UseR"} (in (string->url "//UseR@example.com")))


(expect
  "http://sky@www:801/cgi-bin/finger?name=shriram&host=nw#top"
  (url->string
    #fuzzy_urls.url.Url
        {:scheme "http", :user "sky", :host "www", :port 801,
         :path ["cgi-bin" "finger"],
         :query {:name "shriram", :host "nw"},
         :fragment "top"}))
