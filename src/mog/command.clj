(ns mog.command
  (require [clojure.string :as string]
           [taoensso.timbre :as timbre :include-macros true]))

(def items (atom []))

(defn add-to-list! [item]
  (timbre/debug (str ":: Adding " item " to list"))
  (swap! items conj item)
  (timbre/debug (str ":: List is now " @items))
  (str "I've added " item " to the list"))

(defn display-list []
  (str "Here's the list:\n"
       (string/join "\n" @items)))

(defn unknown-command-response [command]
  (str (rand-nth ["Sorry.  "
                  "Oops.  "
                  "My bad.  "
                  ""])
       "I don't know how to `" command "`."))

(defn parse-input [input]
  (let [[command content] (drop 1 (re-find #"(\S*)\s?(.*)" input))]
    {:command command
     :content content}))

(defn process [input]
  (let [{command :command
         content :content :as parsed-input} (parse-input input)]
    (condp = command
      "add" (add-to-list! content)
      "list" (display-list)
      (unknown-command-response command))))
