(ns mog.core
  (:require [clojure.core.async :as async]
            [mog.comms :refer [comms]]
            [mog.config :refer [config]]
            [mog.logging :refer [logger]]
            [mog.util :as util]
            [mount.core :refer [defstate] :as mount]
            [taoensso.timbre :as timbre :include-macros true])
  (:import java.lang.Thread)
  (:gen-class))

(defn command-loop [comms]
  (async/go-loop [[in out stop] comms]
    (timbre/debug ":: waiting for input")
    (if-let [event (async/<! in)]
      (let [input (:input event)
            res input]
        (timbre/debug ":: event >> " input)
        (timbre/debug ":: => " res)
        (async/>! out (assoc event :mog/response res))
        (recur [in out stop])))))

(defstate main
  :start (command-loop comms))

(defn -main [& args]
  (mount/start #'logger)
  (timbre/info ":: starting with config:" config)
  (mount/start)
  (.join (Thread/currentThread)))
