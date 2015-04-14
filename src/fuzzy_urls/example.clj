(ns fuzzy-urls.example
  (:require [fuzzy-urls.url :refer :all]
            [fuzzy-urls.lens :as lens
             :refer [build-url-lens]]
            [clojure.string :as string]))

; Urls are most easily creatable from strings using string->url.
(def u (string->url "http://en.wikipedia.org/wiki/Philosophy"))

; Urls are composed of a set of fields.
(:scheme u)   ; => "http"
(:user u)     ; => nil
(:host u)     ; => "en.wikipedia.org"
(:port u)     ; => nil
(:path u)     ; => ["wiki" "Philosophy"]
(:query u)    ; => {}
(:fragment u) ; => nil

; Urls may be created manually using the make-url function, which
; accepts keyword arguments.
;
; Unprovided values will automatically be filled in with reasonable
; defaults (nil except in the case of path or query).
(make-url
  :scheme "http"
  :host "graph.facebook.com"
  :path ["v2.3" "feed"])

; Urls do not need to be well-formed to parse properly. They may,
; for example, be missing a protocol. This permits parsing relative
; URLs or bare query parameters.
(string->url "/some/path")
(string->url "?some=query&parameters")

; -----------------------------------------------------------------------------

; Urls can be compared using url lenses. Lenses are effectively applicable
; url comparators. By default, they do a "fuzzy" comparison between
; two urls.
;
; For example, the following comparison returns true.
((build-url-lens)
  (string->url "http://en.wikipedia.org/wiki/Philosophy")
  (string->url "https://en.wikipedia.org/wiki/Philosophy"))

; This is because, by default, http and https schemes are considered equivalent.
; In addition to simple scheme comparison, the default lens ignores the user
; and fragment fields, but compares all others exactly.
;
; The default behavior may be overridden by specifying custom lenses for
; comparing certain fields. For example, the following lens would make the above
; comparison return false.
(build-url-lens
  :scheme lens/exact)

; Url lenses are not terribly useful on their own, but they can also be used to
; filter lists of urls. For example, take the following list of urls:
(def urls
  ["https://en.wikipedia.org/wiki/Philosophy"
   "http://fr.wikipedia.org/wiki/Philosophie"
   "https://fr.wikipedia.org/wiki/Logique"
   "http://en.wikipedia.org/wiki/Humanities"
   "http://en.wikipedia.org/wiki/Help:Category"
   "https://en.wikipedia.org/wiki/Category:Philosophy"
   "http://en.wikipedia.org/wiki/Category:Humanities"])

; One might want to determine a number of things. For example, it could be useful
; to find all the urls on the en subdomain, ignoring differences between schemes.
;
; Lenses are also partially applicable, so they can be used easily with filter.
(filter
  ((build-url-lens
     :host lens/exact
     :path lens/ignore
     :query lens/ignore)
    (string->url "http://en.wikipedia.org/"))
  (map string->url urls))

; This could be further narrowed by using a custom lens to find all pages that
; belong to the same namespace.
(filter
  ((build-url-lens
     :host lens/exact
     :path
     (fn [[_ a] [_ b]]
       (and (.contains a ":")
            (.contains b ":")
            (= (first (string/split a #":"))
               (first (string/split b #":")))))
     :query lens/ignore)
    (string->url "http://en.wikipedia.org/wiki/Category:"))
  (map string->url urls))
