(ns kagaku.factcheck
  "fact-check gate — 科学解説チャンネルの公開前検査。2 段構え:

   1) 決定論 gate（この ns。LLM 判定を gate に置かない — kodomo safety /
      design-quality audit.cljc と同じ思想）: provenance 完全性・sim 結果
      との一致・定数出典・シミュレーション明示・metadata・尺・ラウドネス。
   2) **人間レビュー（無条件 HARD hold）**: 決定論 gate が全 green でも
      publish はオーナーの明示承認（:human-approved {:by .. :at ..}）が
      あるまで :held。AI は計算を堂々と間違える — 数値は sim/定数由来に
      機械強制した上で、なお最終の事実確認は人間が行う（オーナー要件）。
      aozora ADR-2607162200 の governor auto-publish パターンは本チャンネルに
      適用しない。

   入力 facts:
   {:episode     spec                      ; kagaku.scenario 適合
    :sim-results {case-id {:outputs .. :bench ..}}
    :constants   {ref-kw {:quantity n :unit str :source str}}
    :citations   {ref-str {:verified-by str :at str}}   ; 人間確認済み引用
    :render      {:duration-s n :integrated-lufs n :true-peak-dbtp n}
    :metadata    {:made-for-kids bool :description str
                  :sim-disclosure bool :voice kw}
    :human-approved {:by str :at str}}     ; オーナー承認（publish 前提条件）"
  (:require [clojure.string :as str]
            [kagaku.derived :as derived]
            [kagaku.scenario :as scenario]
            [kagaku.simcase :as simcase]))

;; --- 個別チェック（それぞれ {:check .. :ok? bool :detail ..} を返す） --------

(defn check-episode
  [{:keys [episode]}]
  (let [{:keys [valid? errors]} (scenario/validate episode)]
    {:check :episode :ok? valid? :detail {:errors errors}}))

(defn check-sim-consistency
  "sim 出所の claim が実行結果と一致するか（kagaku.simcase/claim-consistency）。"
  [{:keys [episode sim-results]}]
  (let [rows (simcase/claim-consistency (:claims episode) sim-results)]
    {:check :sim-consistency
     :ok? (every? :ok? rows)
     :detail {:failed (vec (remove :ok? rows))}}))

(defn check-constants
  "定数出所の claim が curated constants（resources/constants.edn を :exec が
   ロードして渡す）に存在し、値・単位が一致するか。"
  [{:keys [episode constants]}]
  (let [failed
        (vec
         (for [{:keys [id value source]} (:claims episode)
               :when (= :constant (:kind source))
               :let [c (get constants (:ref source))]
               :when (or (nil? c)
                         (not= (:unit value) (:unit c))
                         (not= (double (:quantity value))
                               (double (:quantity c))))]
           {:claim id :ref (:ref source) :expected value :constant c}))]
    {:check :constants :ok? (empty? failed) :detail {:failed failed}}))

(defn check-derived
  "派生 claim を決定論再計算して宣言値と照合する（kagaku.derived/check）。"
  [{:keys [episode]}]
  (let [{:keys [ok? failed]} (derived/check (:claims episode))]
    {:check :derived :ok? ok? :detail {:failed failed}}))

(defn check-citations
  "引用出所の claim が人間確認済み（:verified-by 付き）の引用台帳にあるか。"
  [{:keys [episode citations]}]
  (let [failed
        (vec
         (for [{:keys [id source]} (:claims episode)
               :when (= :citation (:kind source))
               :let [c (get citations (:ref source))]
               :when (str/blank? (str (:verified-by c)))]
           {:claim id :ref (:ref source)}))]
    {:check :citations :ok? (empty? failed) :detail {:failed failed}}))

(defn check-sim-disclosure
  "誠実性の不変条件: 実験映像は全編シミュレーションであることを視聴者に明示する
   （実写実験と誤認させない）。metadata フラグ + description 中の明記の両方を要求。"
  [{:keys [metadata]}]
  {:check :sim-disclosure
   :ok? (boolean (and (:sim-disclosure metadata)
                      (str/includes? (str (:description metadata))
                                     "シミュレーション")))
   :detail {:sim-disclosure (:sim-disclosure metadata)}})

(defn check-metadata
  "madeForKids=false（一般向け・通常広告）。VOICEVOX 使用時はクレジット必須
   （yukkuri と同一の商用不変条件）。"
  [{:keys [metadata]}]
  (let [voicevox? (= :voicevox (:voice metadata))]
    {:check :metadata
     :ok? (boolean (and (false? (:made-for-kids metadata))
                        (or (not voicevox?)
                            (str/includes? (str (:description metadata))
                                           "VOICEVOX:"))))
     :detail {:made-for-kids (:made-for-kids metadata) :voice (:voice metadata)}}))

(defn check-duration
  [{:keys [render]}]
  (let [d (:duration-s render)]
    {:check :duration
     :ok? (boolean (and (number? d) (<= 120 d 720)))
     :detail {:duration-s d :allowed [120 720]}}))

(defn check-loudness
  "integrated: -20..-12 LUFS、true peak <= -1.0 dBTP（ffmpeg loudnorm 計測）。"
  [{:keys [render]}]
  (let [{:keys [integrated-lufs true-peak-dbtp]} render]
    {:check :loudness
     :ok? (boolean (and (number? integrated-lufs) (number? true-peak-dbtp)
                        (<= -20.0 integrated-lufs -12.0)
                        (<= true-peak-dbtp -1.0)))
     :detail {:integrated-lufs integrated-lufs :true-peak-dbtp true-peak-dbtp}}))

;; --- gate ---------------------------------------------------------------
;;
;; factcheck は 2 段に分かれる（stage 順序の実測で判明した設計上の必然）:
;;   provenance-checks — render 出力に依存しない。:factcheck stage（render 前）で
;;     走り、悪い数値を render 計算に金をかける前に止める。
;;   render-checks — render 後の測定（尺・ラウドネス）を要する。:render-video の
;;     後で走る。render 前に走らせると必ず fail する（:render facts が無いため）。
;; この分割前は checks が両者を混ぜており、実運用順（factcheck が render の前）で
;; provenance が全 green でも loudness 欠如で必ず :rejected になる欠陥があった
;; （full-produce E2E 統合で発火。ADR-2607165000 の factcheck stage 記述を実装で精緻化）。

(defn provenance-checks
  "render 出力に依存しない決定論チェック（:factcheck stage 用）。"
  [facts]
  [(check-episode facts)
   (check-sim-consistency facts)
   (check-constants facts)
   (check-derived facts)
   (check-citations facts)
   (check-sim-disclosure facts)
   (check-metadata facts)])

(defn render-checks
  "render 後の測定チェック（:render-video の後 / publish 前）。"
  [facts]
  [(check-duration facts)
   (check-loudness facts)])

(defn checks
  "全チェック（provenance + render）。render facts が揃った段（publish 判定）で使う。"
  [facts]
  (into (provenance-checks facts) (render-checks facts)))

(defn- decide [results]
  (let [failed (vec (remove :ok? results))]
    {:decision (if (empty? failed) :ready-for-review :rejected)
     :failed failed :results results}))

(defn provenance-gate
  "render 前の HARD gate。provenance 全 green → :ready-for-review、
   1 つでも fail → :rejected（advisory 続行しない）。"
  [facts]
  (decide (provenance-checks facts)))

(defn render-gate
  "render 後の測定 gate（尺・ラウドネス）。"
  [facts]
  (decide (render-checks facts)))

(defn gate
  "provenance + render を合わせた決定論 gate（全 facts が揃った段で使う）。
   後方互換: full facts を渡す既存呼び出しはこれまでどおり動く。"
  [facts]
  (decide (checks facts)))

(defn publish-decision
  "publish の最終判定。決定論 gate（provenance + render）全 green **かつ**
   オーナーの明示承認があるときだけ :publish。承認が無ければ常に :held。"
  [facts]
  (let [{:keys [decision failed]} (gate facts)
        {:keys [by at]} (:human-approved facts)]
    (cond
      (= :rejected decision) {:decision :rejected :failed failed}
      (and (not (str/blank? (str by))) (not (str/blank? (str at))))
      {:decision :publish :approved-by by :approved-at at}
      :else {:decision :held :reason :awaiting-human-review})))
