# full.metrics

[![Clojars Project](https://img.shields.io/clojars/v/fullcontact/full.metrics.svg)](https://clojars.org/fullcontact/full.metrics)
[![Build Status](https://travis-ci.org/fullcontact/full.metrics.svg?branch=master)](https://travis-ci.org/fullcontact/full.metrics)

Clojure application metrics and monitoring sugar.

## Configuration

`full.metrics` reports to [Riemann](http://riemann.io). T

```yaml
riemann:
  host: metrics.yourhost.com
  protocol: udp
  tags: [service-name]
```


Examples:

To measure an execution time of an async method:

```clojure
(defn foo> []
  (go-try-timeit "foo.bar.metric"
    (<! (async-things))))
```

To see these metrics in Riemann, you can then use the following

```
tagged "service-name"
and service =~ "%0.95"
and host = nil
```

There's `thread-try-timeit` for wrapping `full.async/thread-try` requests.
