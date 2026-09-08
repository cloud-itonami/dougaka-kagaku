;; episode 横断 self-audit の CI runner:
;;   nbb --classpath src tools/audit.cljs
;; content/*.edn（*-script.edn を除く）を全部読み、各 episode を kagaku.audit で
;; 検査して findings を表示。1 つでも pass? が false なら exit 1（CI ゲート）。
(ns audit
  (:require [cljs.reader :as reader]
            [kotoba.lang.text :as str]
            [kagaku.audit :as audit]
            ["fs" :as fs]))

(def citations
  (if (fs/existsSync "docs/citations.edn")
    (reader/read-string (fs/readFileSync "docs/citations.edn" "utf8"))
    {}))

(def episode-files
  (->> (fs/readdirSync "content")
       (filter #(str/ends-with? % ".edn"))
       (remove #(str/ends-with? % "-script.edn"))
       sort))

(defn load-edn [p] (reader/read-string (fs/readFileSync p "utf8")))

(def ctxs
  (for [f episode-files
        :let [ep (load-edn (str "content/" f))
              sp (str "content/" (str/replace f ".edn" "-script.edn"))
              script (when (fs/existsSync sp) (load-edn sp))]
        ;; episode spec だけを対象（sim case カタログ等の非-episode edn を除外）
        :when (and (:series ep) (:claims ep))]
    {:file f :episode (assoc ep :topic-id (keyword (str/replace f ".edn" "")))
     :script script :citations citations}))

(println "== kagaku self-audit （" (count ctxs) "episodes ）\n")

(def result (audit/audit-all ctxs))

(doseq [[ctx row] (map vector ctxs (:episodes result))]
  (println (str (if (:pass? row) "✓" "✗") " " (:file ctx)
                "  score=" (.toFixed (* 100 (:score row)) 0) "%"))
  (doseq [f (:findings row)]
    (println "    -" f)))

(println (str "\n== " (if (:all-pass? result) "ALL PASS" "FAILURES")
              "  mean-score=" (.toFixed (* 100 (:mean-score result)) 0) "%"))
(when-not (:all-pass? result) (js/process.exit 1))
