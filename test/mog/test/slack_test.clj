(ns mog.test.slack-test
  (:require [clojure.core.async :as async]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [environ.core :refer [env]]
            [mog.core :as core]
            [mog.logging :refer [logger]]
            [mog.command :as command]
            [mount.core :as mount]
            [taoensso.timbre :as timbre :include-macros true]))

(mount/start #'logger)

(defn setup-test []
  (reset! command/items []))

(defn teardown-test [])

(defn wrap-setup [f]
  (setup-test)
  (f)
  (teardown-test))

(use-fixtures :each wrap-setup)

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
                                    "I don't know how to `test`.")))))
(deftest parse-input
  (testing "parse-input"
    (testing "parses the input into command and content"
      (let [input "add cheese"]
        (is (= {:command "add" :content "cheese"}
               (command/parse-input input)))))
    (testing "all of the content following the command even if there are spaces"
      (let [input "add cheese doodles"]
        (is (= {:command "add" :content "cheese doodles"}
               (command/parse-input input)))))))
(deftest add-command
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

(deftest list-command
  (testing "list command"
    (testing "returns the items in the list separated by newlines"
      (let [[in-chan out-chan stop-fn] (mock-command-loop)
            item1 "cheese"
            item2 "lettuce"
            list-input {:input "list"}
            output (do (command/add-to-list! item1)
                       (command/add-to-list! item2)
                       (async/>!! in-chan list-input)
                       (async/<!! out-chan))]
        (is (= (str "Here's the list:\n"
                    item1 "\n"
                    item2)
               (:mog/response output)))))))

(deftest list-command-empty-list
  (testing "list command"
    (testing "returns List is empty. when list is empty"
      (let [[in-chan out-chan stop-fn] (mock-command-loop)
            list-input {:input "list"}
            output (do (async/>!! in-chan list-input)
                       (async/<!! out-chan))]
        (is (= "The list is empty."
               (:mog/response output)))))))

(deftest list-command-clear-with-empty-list
  (testing "clear command"
    (testing "clears empty lists"
      (let [[in-chan out-chan stop-fn] (mock-command-loop)]
        (let [clear-input {:input "clear"}
              output (do (async/>!! in-chan clear-input)
                         (async/<!! out-chan))]
          (is (= (str "I've cleared the list.")
                 (:mog/response output))))))))

(deftest list-command-clear
  (testing "clear command"
    (testing "clears the list"
      (let [[in-chan out-chan stop-fn] (mock-command-loop)]
        (let [item1 "cheese"
              list-input {:input "list"}
              output (do (command/add-to-list! item1)
                         (async/>!! in-chan list-input)
                         (async/<!! out-chan))]
          (is (= (str "Here's the list:\n"
                      item1)
                 (:mog/response output))))
        (let [clear-input {:input "clear"}
              output (do (async/>!! in-chan clear-input)
                         (async/<!! out-chan))]
          (is (= (str "I've cleared the list.")
                 (:mog/response output))))
        (let [list-input {:input "list"}
              output (do (async/>!! in-chan list-input)
                         (async/<!! out-chan))]
          (is (= "The list is empty."
                 (:mog/response output))))))))
