(ns evernote-todo.core
  (:gen-class)
  (:require 
    [clojure.java.io :as io]
    [clojurenote.enml :as enml]
    [clojurenote.notes :as notes]
    [clojurenote.auth :as auth]))


(defn read-edn-into-objects [dir fname]
  (println "Reading " fname "from" dir)
  (let [path (str dir "/" fname)]
    (when (.exists (io/as-file path))
      (->> path
           slurp
           read-string))))

(defn read-creds [dir]
  (read-edn-into-objects dir "creds.edn"))

(defn list-notebooks [access-token notestore-url]
  (let [user {:access-token access-token :notestore-url notestore-url}]
    (->> (notes/list-notebooks user)
         (map bean))))

(defn find-notebook-by-name [access-token notestore-url notebook-name]
  (->> (list-notebooks access-token notestore-url)
       (filter (comp #{notebook-name} :name))
       first))

(defn make-todo-list [access-token notestore-url items notebook-name]
  (let [user {:access-token access-token :notestore-url notestore-url}
        nb-guid (-> (find-notebook-by-name access-token notestore-url notebook-name)
                    :guid)
        enml-content (->> (map #(str "<en-todo/>" % "<br/>") items)
                          (clojure.string/join " "))
        _ (println enml-content)
        datefmt (java.text.SimpleDateFormat. "yyyy-MM-dd")
        now (java.util.Date.)
        title (str "Todo: " (.format datefmt now))]
    (notes/write-note user nb-guid title (enml/create-enml-document enml-content))))

(defn -main
  "I don't do a whole lot ... yet."
  [& [dir]]
  (let [;dir (System/getProperty "user.dir")
        {:keys [access-token notestore-url]} (read-creds dir)
        todo-items (read-edn-into-objects dir "todo.edn")]
    (if (and access-token notestore-url)
      (if-not (empty? todo-items)
        (make-todo-list access-token notestore-url todo-items "Management")
        (println "Empty todo list.  Nothing to do."))
      (println "Missing creds.edn file.  You need to get a developer token and notestore, and store them in creds.edn"))))
