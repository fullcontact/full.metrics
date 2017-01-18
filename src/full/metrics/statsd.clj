(ns full.metrics.statsd
  (:require [full.core.config :refer [opt]]
            [full.core.log :as log])
  (:import (com.timgroup.statsd NonBlockingStatsDClient NoOpStatsDClient)))

(def host (opt [:statsd :host] :default nil))
(def port (opt [:statsd :port] :default nil))
(def prefix (opt [:statsd :prefix] :default nil))

(defn get-client
  [prefix host port]
  (if (and prefix host port)
    (do
      (log/info "Initializing" prefix "StatsD Client to" host "over" port)
      (NonBlockingStatsDClient. prefix host port))
    (NoOpStatsDClient.)))

(def client
  (delay (get-client @prefix @host @port)))

(defn timing
  ([k v]
   (.recordExecutionTime @client k v 1.0)))

(defn gauge
  ([k v]
   (.gauge @client k v)))

(defn increment
  ([k]
   (.increment @client k)))

(defn decrement
  ([k]
   (.decrement @client k)))

(defmacro timeit
  [k & body]
  `(if (and @prefix @host @port)
     (let [start# (System/currentTimeMillis)
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
           (throw error#))
         (do (increment success#)
             result#)))
     ~@body))