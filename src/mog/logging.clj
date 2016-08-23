(ns mog.logging
  (:require [environ.core :refer [env]]
            [mount.core :refer [defstate] :as mount]
            [taoensso.timbre :as timbre :include-macros true]
            [taoensso.timbre.appenders.core :as appenders]))

(defn setup []
  (let [{fname :fname
         standard-out :standard-out
         :or {fname "log/mog.log"
              standard-out false}} {:fname (:mog-log-file env)
                                    :standard-out (boolean (:mog-standard-out env))}]
    (timbre/merge-config! {:appenders
                           {:spit (appenders/spit-appender
                                   {:fname fname})}})
    (timbre/merge-config! {:appenders
                           {:println {:enabled? standard-out}}})))

(defstate logger
  :start (setup))
