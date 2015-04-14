(ns fuzzy-urls.lens
  (:require [fuzzy-urls.url :refer :all]))

(declare fuzzy-equal?)

(defrecord UrlLens [scheme-fn user-fn host-fn port-fn path-fn query-fn fragment-fn]
  ; lenses are applicable
  clojure.lang.IFn
  ; they're also auto-curried for use in filter and similar functions
  (invoke [this a] (fn [b] (this a b)))
  (invoke [this a b] (fuzzy-equal? this a b))
  ; this is required for implementing IFn properly
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))

; General lenses and lens constructors
; -----------------------------------------------------------------------------

(defn ignore
  "A simple lens that ignores its arguments and always returns true."
  [& _] true)

(defn exact
  "A simple lens that performs exact equality on its arguments."
  [& args] (apply = args))

(defn in-set
  "A lens constructor that produces a lens that checks if its two arguments
  exist within the same set."
  [sets]
  (fn [a b]
    ; get all the sets that contain a...
    (let [relevant-sets (filter #(contains? % a) sets)]
      ; ...then check if any of them contain b
      (boolean (some #(contains? % b) relevant-sets)))))

; Convenience lenses
; -----------------------------------------------------------------------------

(def common-schemes
  "A lens that detects common similar schemes, such as HTTP and HTTPS."
  (in-set
    #{#{"http" "https"}
      #{"ws" "wss"}}))

; -----------------------------------------------------------------------------

(defn build-url-lens
  "Creates a fuzzy url lens, using default values for unprovided lenses."
  [& {:keys [scheme user host port path query fragment]
      :or {scheme common-schemes
           user ignore
           host exact
           port exact
           path exact
           query exact
           fragment ignore}}]
  (->UrlLens scheme user host port path query fragment))

(defn fuzzy-equal? [lens a b]
  {:pre [(instance? UrlLens lens)
         (url? a) (url? b)]}
  (boolean
    (and
      ((:scheme-fn lens)   (:scheme a)   (:scheme b))
      ((:user-fn lens)     (:user a)     (:user b))
      ((:host-fn lens)     (:host a)     (:host b))
      ((:port-fn lens)     (:port a)     (:port b))
      ((:path-fn lens)     (:path a)     (:path b))
      ((:query-fn lens)    (:query a)    (:query b))
      ((:fragment-fn lens) (:fragment a) (:fragment b)))))
