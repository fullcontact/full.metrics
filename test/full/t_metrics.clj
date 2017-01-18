(ns full.t-metrics
  (:require [midje.sweet :refer :all]
            [full.metrics.riemann :as rmn]
            [full.metrics.statsd :as statsd]
            [full.metrics :refer :all]))

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
          (statsd/get-client "prefix" "localhost" 1234)
          (timeit "k" (inc 0))) => 1))