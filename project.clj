(defproject fullcontact/full.metrics "0.12.0"
  :description "Clojure application metrics and monitoring sugar"
  :url "https://github.com/fullcontact/full.metrics"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :deploy-repositories [["releases" {:url "https://clojars.org/repo/" :creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [riemann-clojure-client "0.4.1" :exclusions [org.clojure/tools.logging
                                                              org.slf4j/slf4j-api
                                                              com.google.protobuf/protobuf-java]]
                 [com.google.protobuf/protobuf-java "3.1.0"]
                 [clj-statsd "0.4.0"]
                 [com.climate/clj-newrelic "0.2.1"]
                 [fullcontact/full.async "1.1.0"]
                 [fullcontact/full.core "1.1.1" :exclusions [org.clojure/clojurescript]]]
  :aot [full.metrics]  ; clojure new relic extension doesn't work when AOT'ed.
  :plugins [[lein-midje "3.2.2"]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :profiles {:dev {:dependencies [[midje "1.9.9"]]}})
