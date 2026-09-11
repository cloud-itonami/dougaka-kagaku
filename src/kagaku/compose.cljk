(ns kagaku.compose
  "compose stage — 次に作る topic の選定。純関数・IO なし
   （series カタログと produced 履歴は呼び出し側が渡す）。
   yukkuri topics.cljc の pick-topic（priorityScore 順 + 既出除外）を移植し、
   kagaku の series.edn 形（{:series {:topics [{:id :q :priority}]}}）に合わせ、
   さらに **series ローテーション**（直近と同じ series を軽く後回し）を足す —
   チャンネルが 5 series を偏りなく回すため。

   決定論のみ（乱数・時刻を使わない — 同じ入力は常に同じ選定）。tie-break は
   series 名 → topic id の辞書順で安定化する。"
  )

(defn candidates
  "series カタログを {:series :topic-id :question :priority :sim-kinds} の
   平坦なベクタに展開する。"
  [catalog]
  (vec
   (for [[series {:keys [topics sim-kinds]}] catalog
         {:keys [id q priority]} topics]
     {:series series
      :topic-id id
      :question q
      :priority (or priority 0)
      :sim-kinds sim-kinds})))

(defn- effective-score
  "priority に series ローテーションの補正を掛けた実効スコア。
   last-series と同じ series は penalty を引く（既定 15 — priority の粒度が
   5〜90 なので、僅差の別 series を優先しつつ、圧倒的高 priority は覆さない）。"
  [{:keys [series priority]} last-series penalty]
  (if (and last-series (= series last-series))
    (- priority penalty)
    priority))

(defn pick-topic
  "次に作る topic を 1 件選ぶ（無ければ nil）。
   opts:
     :produced-ids  既に作った topic-id の集合（除外）
     :series        指定時はその series に限定
     :last-series   直近に作った series（ローテーション補正の対象）
     :rotation-penalty  直近 series への減点（既定 15）"
  ([catalog] (pick-topic catalog {}))
  ([catalog {:keys [produced-ids series last-series rotation-penalty]
             :or {produced-ids #{} rotation-penalty 15}}]
   (let [pool (cond->> (candidates catalog)
                true (remove #(contains? produced-ids (:topic-id %)))
                series (filter #(= series (:series %))))]
     (when (seq pool)
       (->> pool
            ;; 実効スコア降順 → tie は series 名 → topic-id で安定化
            (sort-by (juxt #(- (effective-score % last-series rotation-penalty))
                           #(name (:series %))
                           #(name (:topic-id %))))
            first)))))

(defn to-episode-seed
  "選定 topic → episode spec の骨格（compose stage の出力）。
   これは spec の種であって完成 episode ではない — claims / sim-cases /
   台本は後続 stage（design-sim / generate-script）が埋める。
   duration は series ごとの既定（three-min-math は短尺）。"
  [{:keys [series topic-id question sim-kinds]}]
  {:series series
   :topic-id topic-id
   :question question
   :sim-kinds sim-kinds
   :duration-s (if (= series :three-min-math) 180 300)
   :claims []
   :sim-cases []
   :beats []})

(defn rank
  "デバッグ/可視化用: 全候補を実効スコア順に並べて返す。"
  ([catalog] (rank catalog {}))
  ([catalog {:keys [produced-ids last-series rotation-penalty]
             :or {produced-ids #{} rotation-penalty 15}}]
   (->> (candidates catalog)
        (remove #(contains? produced-ids (:topic-id %)))
        (sort-by (juxt #(- (effective-score % last-series rotation-penalty))
                       #(name (:series %))
                       #(name (:topic-id %))))
        vec)))
