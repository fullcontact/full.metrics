(ns full.t-metrics
  (:require [midje.sweet :refer :all]
            [full.metrics.riemann :as rmn]
            [full.metrics.statsd :as statsd]
            [full.metrics :refer :all]
            [full.async :refer [<??]])
  (:import (clojure.lang ExceptionInfo)))

(facts
  "Test timeit body evaluated with no configuration."
  (fact (timeit "k" (inc 0)) => 1))

(facts
  "Test timeit body evaluated with riemann configuration."
  (fact (do
          (rmn/get-client {:protocol "udp" :config {:host "127.0.0.1"}})
          (timeit "k" (inc 0))) => 1))

(facts
  "Test timeit body evaluated with riemann and statsd configuration."
  (fact (do
          (rmn/get-client {:protocol "udp" :config {:host "127.0.0.1"}})
          (timeit "k" (inc 0))) => 1))

(facts
  "Test go-try-timeit body evaluated with riemann and statsd configuration."
  (fact (do
          (rmn/get-client {:protocol "udp" :config {:host "127.0.0.1"}})
          (<?? (go-try-timeit "k"
                              (inc 0)
                              (inc 0)))) => 1)

  (fact (do
          (rmn/get-client {:protocol "udp" :config {:host "127.0.0.1"}})
          (->> (try
                 (<?? (go-try-timeit "k" (throw (Exception.))))
               (catch Exception e
                 (str e))))) => "clojure.lang.ExceptionInfo: java.lang.Exception {}"))
