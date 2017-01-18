(ns full.metrics.riemann
  (:require [full.core.sugar :refer :all]
            [full.core.log :as log]
            [full.core.config :refer [opt]]
            [riemann.client :as rmn]
            [riemann.codec :as rc]
            [full.async :refer [go-try thread-try]])
  (:import (java.net InetAddress)
           (org.slf4j LoggerFactory))
  (:refer-clojure :exclude [send]))

;;; CONFIG ;;;

(def riemann-config (opt :riemann :default nil))
(def protocol (opt [:riemann :protocol] :default "udp"))
(def batch-size (opt [:riemann :batch-size] :default nil))
(def tags (opt [:riemann :tags] :default nil))

(defn get-client
  [{:keys [protocol config batch-size]}]
  (log/info "Connecting to Riemann server" (:host config) "via" protocol)
  (let [localhost (.. InetAddress getLocalHost getHostName)]
    ;;; Don't do dumb things while sending riemann events.
    ;;; https://github.com/aphyr/riemann-clojure-client/pull/14
    (alter-var-root
      #'rc/encode-client-pb-event
      (constantly
        #(-> %
             (rc/assoc-default :time (/ (System/currentTimeMillis) 1000))
             (rc/assoc-default :host localhost)
             rc/encode-pb-event))))
  (let [rclient (or (when (= "tcp" protocol)
                      (rmn/tcp-client config))
                    (rmn/udp-client config))]
    (if batch-size
      (do
        (log/info "Enabling RiemannBatchClient with size" batch-size)
        (rmn/batch-client rclient batch-size))
      rclient)))

(def client (delay (get-client {:protocol @protocol
                                :config @riemann-config
                                :batch-size @batch-size})))


;;; EVENT PUT ;;;

(defn normalize [event]
  (cond-> event
          ; if event is string convert it to {:service event}
          (string? event) (->> (hash-map :service))
          @tags (assoc :tags (concat (:tags event []) @tags))))

(defn send-event [event]
  (when @riemann-config
    (try
      (rmn/send-event @client event)
      (catch Exception e
        (log/error e "error sending event" event)))))

(defn- send-events [events]
  (when @riemann-config
    (try
      (rmn/send-events @client events)
      (catch Exception e
        (log/error e "error sending events" events)))))

(defn- log-event [event]
  (-> (str "full.metrics." (:service event))
      (LoggerFactory/getLogger)
      (.debug (pr-str event))))

(defn track
  "Send an event over client. Requests acknowledgement from the Riemann
   server by default. If ack is false, sends in fire-and-forget mode."
  ([event]
   (doto (normalize event)
     (log-event)
     (send-event))))

(defn track*
  ([events]
   (let [events (map normalize events)]
     (doseq [event events] (log-event event))
     (send-events events))))

;;; EVENT PUT SUGAR ;;;

(defn wrap-event [event]
  (if (string? event)
    {:service event}
    event))

(defn gauge
  "A gauge is an instantaneous measurement of a value. For example, we may want
  to measure the number of pending jobs in a queue."
  [event value]
  (-> (wrap-event event)
      (assoc :metric value
             :tags ["gauge"])
      (track)))

(def timeit-gauges (atom {}))

(defn update-timeit-gauge [key f]
  (get (swap! timeit-gauges update-in [key] (fnil f 0)) key))

(defmacro timeit
  [event & body]
  `(if @riemann-config
     (let [event# ~event
           start-time# (time-bookmark)
           event# (wrap-event event#)
           g# (update-in event# [:service] str "/gauge")]
       (gauge g# (update-timeit-gauge (:service event#) inc))
       (let [res# (try ~@body (catch Throwable t# t#))
             fail# (instance? Throwable res#)]
         (track (assoc event# :metric (ellapsed-time start-time#)
                              :tags (conj (or (:tags event#) []) "timeit")
                              :state (if fail# "critical" "ok")
                              :description (when fail# (str res#))))
         (gauge g# (update-timeit-gauge (:service event#) dec))
         (if fail#
           (throw res#)
           res#)))
     ~@body))

(defn increment
  [event]
  (-> (wrap-event event)
      (assoc :metric 1 :tags ["increment"])
      (track)))

(defn query
  "Query the server for events in the index. Returns a list of events."
  [string]
  {:pre [@riemann-config]}
  (rmn/query @client string))

