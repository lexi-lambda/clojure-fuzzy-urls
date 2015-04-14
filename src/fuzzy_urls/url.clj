(ns fuzzy-urls.url
  (:require [clojure.string :as string]
            [clojure.walk :refer (keywordize-keys)]))

(defn- maybe
  "Helper function for creating predicates that might be nil."
  [pred]
  (some-fn nil? pred))

; A Url is composed of seven fields:
;
; http://sky@www:801/cgi-bin/finger?name=shriram&host=nw#top
; {1-}   {2} {3} {4}{---5---------} {----6-------------} {7}
;
; 1 = scheme, 2 = user, 3 = host, 4 = port,
; 5 = path (two elements), 6 = query, 7 = fragment
;
; The types of these fields are as follows:
; - scheme: string / nil
; - user: string / nil
; - host: string / nil
; - port: int / nil
; - path: [string]
; - query: {keyword, string / nil}
; - fragment: string / nil

(defrecord Url [scheme user host port path query fragment])
(defn url? [x] (instance? Url x))

(defn make-url
  "Convenience constructor for Url."
  [& {:keys [scheme user host port path query fragment]
      :or {path []
           query {}}}]
  (->Url scheme user host port path query fragment))

; This regex is taken mostly verbatim from RFC 3986, Appendix B.
; It has been modified slightly to disable capturing of uninteresting groups.
; The resulting capture groups correspond to the following fields:
; (1) scheme (without trailing colon)
; (2) user+host+port (no leading or trailing slashes)
; (3) path (with leading slash)
; (4) query string (without leading question mark)
; (5) fragment (without leading hash mark)
(def ^:private url-pattern
  #"^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\?([^#]*))?(?:#(.*))?")

(def ^:private host-pattern
  #"^(?:([^@]*)@)?(.+?)(?::(\d+))?$")


(defn- path-string->list
  "Converts a path string (which may be nil) to a vector of path elements."
  [path]
  {:pre  [((maybe string?) path)]
   :post [(vector? %) (every? string? %)]}
  ; drop the first element of the path because it contains a leading slash
  (into [] (and path (rest (string/split path #"/")))))

(defn- query-map?
  "A predicate for determining if a map is a valid representation of a query string."
  [query]
  {:pre [(map? query)]}
  (every?
    (fn [[k v]]
      (and (keyword? k)
           ((maybe string?) v)))
    query))

(defn- query-string->map
  "Converts a query string (which may be nil) to a map representation."
  [query]
  {:pre  [((maybe string?) query)]
   :post [(map? %) (query-map? %)]}
  (if-not query
    {}
    (let [elements (string/split query #"&")
          pairs (map #(string/split % #"=" 2) elements)]
      (keywordize-keys  (into {} ; this is necessary when v is nil
                              (for [[k v] pairs] [k v]))))))

(defn- query-map->string
  "Converts a map representation of a query string to a string."
  [query]
  {:pre  [(query-map? query)]
   :post [(string? %)]}
  (string/join
    "&"
    (for [[k v] query]
      (str (name k) "=" v))))

(defn string->url
  "Parses a string into a url. Malformed or incomplete urls are supported,
  and the relevant fields will be left nil."
  [string]
  {:pre  [(string? string)]
   :post [(url? %)]}
  (let [[_ scheme user+host+port path query fragment]
        (re-matches url-pattern string)
        [_ user host port]
        (if user+host+port (re-matches host-pattern user+host+port) [])]
    (->Url scheme user host
           (and port (Integer/parseInt port))
           (path-string->list path)
           (query-string->map query) fragment)))

(defn url->string
  "Gets the string representation of a url. Missing portions are not included
  in the result."
  [url]
  {:pre  [(url? url)]
   :post [(string? %)]}
  (let [{:keys [scheme user host port path query fragment]} url]
    (str
      (when scheme (str scheme "://"))
      (when user (str user "@"))
      host
      (when port (str ":" port))
      (when-not (empty? path) (str "/" (string/join "/" path)))
      (when-not (empty? query) (str "?" (query-map->string query)))
      (when fragment (str "#" fragment)))))

