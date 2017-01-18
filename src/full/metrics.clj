(ns full.metrics
  (:require [full.async :refer [go-try thread-try]]
            [full.metrics.riemann]
            [full.metrics.statsd]))

(defmacro timeit
  [event & body]
  `(->> (full.metrics.statsd/timeit ~event ~@body)
        (full.metrics.riemann/timeit ~event)))

(defmacro go-try-timeit
  [event & body]
  `(go-try (timeit ~event ~@body)))

(defmacro thread-try-timeit
  [event & body]
  `(thread-try (timeit ~event ~@body)))

(defn wrap-timeit
  [event f]
  (fn
    ; several defintions to optimize performance
    ([a] (timeit event (f a)))
    ([a b] (timeit event (f a b)))
    ([a b c] (timeit event (f a b c)))
    ([a b c d & more]
     (timeit event (apply f (cons a (cons b (cons c (cons d more)))))))))

(defn timing
  ([k v]
   (full.metrics.statsd/timing k v)))

(defn gauge
  [k v]
  (full.metrics.riemann/gauge k v)
  (full.metrics.statsd/gauge k v))

(defn increment
  [k]
  (full.metrics.riemann/increment k)
  (full.metrics.statsd/increment k))

(defn track
  ([k]
   (full.metrics.riemann/track k)))

(defn track*
  ([ks]
   (full.metrics.riemann/track* ks)))