;; Phase A E2E runner（nbb 第一経路）:
;;   nbb --classpath src tools/run_episode.cljs content/tsuki-half-distance.edn [--append]
;; episode spec を読み、rom で解ける sim case を実行 → claim 整合 / 定数出典を
;; 検証してレポートする。--append で bench datom を docs/sim-benchmark-ledger.edn
;; に追記（append-only 台帳。既定は dry-run 表示のみ）。
;; render 後の facts（loudness / metadata）を要する full gate は
;; kagaku.factcheck/gate が担い、ここでは扱わない（未レンダーの段で
;; gate :rejected を「失敗」と誤読させないため）。
(ns run-episode
  (:require [cljs.reader :as reader]
            [clojure.pprint :refer [pprint]]
            [kagaku.rom :as rom]
            [kagaku.scenario :as scenario]
            [kagaku.simcase :as simcase]
            ["fs" :as fs]))

(def args (vec *command-line-args*))
(def episode-path (first (remove #(= "--append" %) args)))
(def append? (some #(= "--append" %) args))

(when-not episode-path
  (println "usage: nbb --classpath src tools/run_episode.cljs <episode.edn> [--append]")
  (js/process.exit 1))

(def episode (reader/read-string (fs/readFileSync episode-path "utf8")))
(def constants (reader/read-string (fs/readFileSync "resources/constants.edn" "utf8")))

(println "== episode:" (:question episode))

;; 1) spec 検証
(let [{:keys [valid? errors]} (scenario/validate episode)]
  (println "\n-- scenario/validate:" (if valid? "OK" "FAIL"))
  (when-not valid? (pprint errors) (js/process.exit 1)))

;; 2) sim 実行（rom で解ける case のみ。計時は runner が行う）
(def results
  (into {}
        (for [{:keys [id] :as c} (:sim-cases episode)
              :when (contains? rom/rom-kinds (get-in c [:solver :kind]))]
          (let [t0 (js/Date.now)
                r (rom/solve c constants)
                wall (- (js/Date.now) t0)]
            [id (assoc r :bench {:wall-ms wall :runtime :nbb
                                 :steps (get-in c [:budget :steps])})]))))

(println "\n-- sim results:")
(doseq [[id r] results]
  (println " " id)
  (doseq [[k v] (:outputs r)]
    (println "   " k "=" (:quantity v) (:unit v)))
  (println "    bench:" (pr-str (:bench r))))

;; 3) claim ⇔ sim 整合
(def rows (simcase/claim-consistency (:claims episode) results))
(println "\n-- claim-consistency:")
(doseq [{:keys [claim ok? reason expected actual]} rows]
  (println " " claim (if ok? "OK" (str "FAIL(" reason ")"))
           "expected=" (pr-str (:quantity expected))
           "actual=" (pr-str (:quantity actual))))

;; 4) 定数出典
(println "\n-- constants provenance:")
(def const-fails
  (vec (for [{:keys [id value source]} (:claims episode)
             :when (= :constant (:kind source))
             :let [c (get constants (:ref source))]
             :when (or (nil? c)
                       (not= (:unit value) (:unit c))
                       (not= (double (:quantity value)) (double (:quantity c))))]
         id)))
(doseq [{:keys [id source]} (:claims episode)
        :when (= :constant (:kind source))]
  (println " " id (if (some #{id} const-fails) "FAIL" "OK")
           "<-" (:ref source) (str "(" (:source (get constants (:ref source))) ")")))

;; 5) bench 台帳
(def episode-id (-> episode-path (.split "/") last (.replace ".edn" "") keyword))
(def datoms (simcase/bench-datoms episode-id (:sim-cases episode) results))
(def stamped (mapv #(assoc % :kagaku.bench/at (.toISOString (js/Date.))) datoms))
(println "\n-- bench datoms" (if append? "(appending to docs/sim-benchmark-ledger.edn)" "(dry-run; --append to record)"))
(doseq [d stamped] (prn d))
(when append?
  (fs/appendFileSync "docs/sim-benchmark-ledger.edn"
                     (apply str (map #(str (pr-str %) "\n") stamped))))

(def all-ok? (and (every? :ok? rows) (empty? const-fails)))
(println "\n== claims verdict:" (if all-ok? "ALL OK" "FAILURES") "(full gate は render 後の facts で kagaku.factcheck/gate)")
(when-not all-ok? (js/process.exit 1))
