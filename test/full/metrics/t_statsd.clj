(ns full.metrics.t-statsd
  (:require [midje.sweet :refer :all]
            [full.metrics.statsd :refer :all]))

(facts
  "Test batch client returned when size specified."
  (fact (-> @client
            (type)
            (str)) => "class com.timgroup.statsd.NoOpStatsDClient")

  (fact (-> (get-client "derp" "localhost" 1234)
            (type)
            (str)) => "class com.timgroup.statsd.NonBlockingStatsDClient"))
