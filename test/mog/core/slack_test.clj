(ns mog.core.slack-test
  (:require [clojure.core.async :as async]
            [clojure.test :refer [deftest testing is]]
            [environ.core :refer [env]]
            [mog.core :as core]
            [mog.logging :refer [logger]]
            [mog.command :as command]
            [mount.core :as mount]
            [taoensso.timbre :as timbre :include-macros true]))

(mount/start #'logger)

(defn mock-command-loop []
  (let [in (async/chan)
        out (async/chan)
        stop (fn []
               (async/close! in)
               (async/close! out))]
    (core/command-loop [in out stop])
    [in out stop]))

(deftest command-loop
  (testing "commands that don't match return 'I don't know how to...'"
    (let [[in-chan out-chan stop-fn] (mock-command-loop)
          input {:input "test"}
          output (do (async/>!! in-chan input)
                     (async/<!! out-chan))]
      (is (clojure.string/includes? (:mog/response output)
                                    "I don't know how to `test`."))))
  (testing "parse-input"
    (testing "parses the input into command and content"
      (let [input "add cheese"]
        (is (= {:command "add" :content "cheese"}
               (command/parse-input input))))))
  (testing "add command"
    (let [[in-chan out-chan stop-fn] (mock-command-loop)
          item "cheese"
          input {:input (str "add " item)}
          output (do (async/>!! in-chan input)
                     (async/<!! out-chan))]
      (testing "returns the proper message"
        (is (= "I've added cheese to the list" (:mog/response output))))
      (testing "adds the item to the list"
        (is (= ["cheese"] @command/items))))))
