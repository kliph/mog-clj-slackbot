(ns mog.comms
  (:require [mog.config :refer [config]]
            [mog.util :as util]
            [mount.core :refer [defstate]]
            [taoensso.timbre :as timbre :include-macros true]))

(defn make-comm [id config]
  (let [f (util/kw->fn id)]
    (f config)))

(defn inst-comm []
  (timbre/info ":: starting with config:" config)
  (timbre/info ":: building com:" (:comm config))
  (make-comm (:comm config) config))

(defn stop-comm [comm]
  (let [[in-chan out-chan stop-fn] comm]
    (stop-fn)))

(defstate comms
  :start (inst-comm)
  :stop (stop-comm comms))
