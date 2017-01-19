(ns full.metrics.statsd
  "Send metrics to statsd."
  (:require [clojure.string :as str]
            [full.core.config :refer [opt]]
            [full.core.log :as log]
            [clj-statsd :as statsd])
  (:import [java.util Random])
  (:import [java.net DatagramPacket DatagramSocket InetAddress]))


(def host (opt [:statsd :host] :default nil))
(def port (opt [:statsd :port] :default nil))
(def prefix (opt [:statsd :prefix] :default nil))
; clj-statsd will send the randomized percentage of packets / sample rate defined here.
(def sample-rate (opt [:statsd :sample-rate] :default 1.0))

(def config (delay
              (when (and @host @port @prefix)
                (log/info "Initializing" @prefix "StatsD Client to" @host "over" @port)
                (statsd/setup @host @port :prefix (str @prefix ".")))))

;;; COUNTERS ;;;

(def incrementor (delay
                   @config
                   (if @sample-rate
                     (fn [k] (statsd/increment k 1 @sample-rate))
                     (fn [k] (statsd/increment k)))))

(defn increment
  ([k]
   (@incrementor k)
   nil))

;;; TIMERS ;;;

(def timer (delay
             @config
             (if @sample-rate
               (fn [k v] (statsd/timing k v @sample-rate))
               (fn [k v] (statsd/timing k v)))))

(defn timing
  ([k v]
   (@timer k v)
   nil))

(defmacro timeit
  [k & body]
  `(let [start# (System/currentTimeMillis)
         result# (try ~@body (catch Throwable t# t#))
         end# (System/currentTimeMillis)
         error# (instance? Throwable result#)
         fail# (str ~k ".error")
         success# (str ~k ".success")]
     (timing ~k (- end# start#))
     (increment ~k)
     (if error#
       (do
         (increment fail#)
         (throw result#))
       (do (increment success#)
           result#))))

;;; GAUGES ;;;

(def gauger (delay
              @config
              (if @sample-rate
                (fn [k v] (statsd/gauge k v @sample-rate))
                (fn [k v] (statsd/gauge k v)))))

(defn gauge
  ([k v]
   (@gauger k v)
   nil))





