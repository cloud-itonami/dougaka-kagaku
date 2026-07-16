(ns kagaku.scenario
  "エピソード spec（1 動画 = 1 episode）の検証。純データ、外部 IO なし。

   本チャンネルの中心不変条件は **LLM に算術をさせない**:
   台本中のすべての数値主張（claim）は、出所（provenance）を
   3 種のいずれかで持たなければならない —
     :sim      — kotoba-lang sim スタックの solver 実行結果（datom）
     :constant — resources/constants.edn の出典付き物理定数
     :citation — research-brief で人間が確認した出典付き引用
   provenance の完全性は kagaku.factcheck の HARD gate が機械強制する。

   spec 形:
   {:series   kw                       ; series カタログのいずれか
    :question str                      ; 動画 1 本が答える問い
    :duration-s n                      ; 目標尺
    :claims   [{:id kw :text str
                :value {:quantity n :unit str}   ; 数値主張のみ必須
                :source {:kind :sim :case kw :output kw}
                        | {:kind :constant :ref kw}
                        | {:kind :citation :ref str}}]
    :sim-cases [case]                  ; kagaku.simcase/validate-case に適合
    :beats    [str]}                   ; 絵コンテ/展開ビート（自由記述）"
  (:require [clojure.string :as str]))

(def series
  "series カタログ（SSoT。resources/series.edn は topic カタログ側）。"
  #{:moon-approach        ; もし月が地球に近づいたら（軌道・潮汐 what-if）
    :animal-power         ; 動物の身体能力比較（スケーリング則・生体力学）
    :everyday-mechanism   ; 身近な物の仕組み（CFD / 構造）
    :future-tech          ; 未来技術のシミュレーション（aero / echem / 複合）
    :three-min-math})     ; 3分で理解する数学・物理（数値実験）

(def source-kinds #{:sim :constant :citation :derived})

(defn- claim-errors [{:keys [id text value source] :as _claim}]
  (cond-> []
    (not (keyword? id)) (conj [:claim/id-missing id])
    (str/blank? (str text)) (conj [:claim/text-missing id])
    (and value (not (and (number? (:quantity value))
                         (string? (:unit value)))))
    (conj [:claim/value-malformed id])
    (and value (not (contains? source-kinds (:kind source))))
    (conj [:claim/provenance-missing id])
    (and (= :sim (:kind source))
         (not (and (keyword? (:case source)) (keyword? (:output source)))))
    (conj [:claim/sim-ref-malformed id])
    (and (= :constant (:kind source)) (not (keyword? (:ref source))))
    (conj [:claim/constant-ref-malformed id])
    (and (= :citation (:kind source)) (str/blank? (str (:ref source))))
    (conj [:claim/citation-ref-malformed id])
    (and (= :derived (:kind source))
         (not (and (keyword? (:op source)) (sequential? (:from source))
                   (seq (:from source)))))
    (conj [:claim/derived-ref-malformed id])))

(defn validate
  "episode spec を検証して {:valid? bool :errors [..]} を返す。決定論のみ。"
  [{:keys [series duration-s question claims sim-cases] :as _spec}]
  (let [case-ids (set (map :id sim-cases))
        errors
        (-> []
            (cond->
              (not (contains? kagaku.scenario/series series))
              (conj [:series/unknown series])

              (str/blank? (str question))
              (conj [:question/missing])

              (not (and (number? duration-s) (<= 120 duration-s 720)))
              (conj [:duration/out-of-range duration-s])

              (empty? claims)
              (conj [:claims/empty]))
            (into (mapcat claim-errors claims))
            (into (keep (fn [{:keys [id source]}]
                          (when (and (= :sim (:kind source))
                                     (not (contains? case-ids (:case source))))
                            [:claim/sim-case-unknown id (:case source)]))
                        claims))
            (into (let [claim-ids (set (map :id claims))]
                    (mapcat (fn [{:keys [id source]}]
                              (when (= :derived (:kind source))
                                (concat
                                 (keep (fn [f]
                                         (when-not (contains? claim-ids f)
                                           [:claim/derived-from-unknown id f]))
                                       (:from source))
                                 ;; 自己参照は最小の循環。全 SCC 検出まではやらず
                                 ;; 直接自己参照だけ弾く（多段循環は evaluate が
                                 ;; :from-claim-missing で止まるが値は出ない）。
                                 (when (some #{id} (:from source))
                                   [[:claim/derived-self-reference id]]))))
                            claims)))
            (into (let [ids (map :id claims)]
                    (when (not= (count ids) (count (set ids)))
                      [[:claims/duplicate-ids]]))))]
    {:valid? (empty? errors) :errors errors}))

(defn sim-claims
  "sim を出所とする claim だけを返す。"
  [claims]
  (filter #(= :sim (get-in % [:source :kind])) claims))
