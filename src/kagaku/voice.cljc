(ns kagaku.voice
  "synthesize-voice stage の per-line VOICEVOX request plan。純データ・IO なし。
   yukkuri voicevox.cljc の純ロジック（style_id カタログ / emotion→style /
   synthesize-plan / クレジット生成）を移植し、kagaku 向けに調整:

   - 入力は **展開済み台本**（kagaku.script/expand 後 — placeholder は検証済み
     claim 値に転記済み、数字直書きは無い）。plan は line ごとに 1 リクエスト。
   - 話者は yukkuri と同じ既定: left=四国めたん(2) / right=ずんだもん(3)。
   - 語り口は「ゆっくり実況」ではなく通常の科学解説なので speed 既定 1.0。
   - VOICEVOX 商用クレジット（description 必須）を plan から機械生成し、
     factcheck の metadata チェック（description に 'VOICEVOX:' 必須）を
     満たせるようにする — クレジットの取りこぼしを構造で防ぐ。

   実 IO（/audio_query → /synthesis の 2 段 POST、または ADR-2607131645 の
   murakumo 公開 /v1/audio/speech）は :exec 側。ここは何を叩くかの計画のみ。"
  (:require [clojure.string :as str]))

;; ── style_id カタログ（yukkuri と同期。emotion off が既定＝声色を安定させる）──
(def speaker-left-default 2)   ; 四国めたん ノーマル
(def speaker-right-default 3)  ; ずんだもん ノーマル

(def emotion-style
  "base style_id → emotion → style_id（voicevox_engine 0.21 系）。
   kagaku.script の emotion 集合（normal/happy/surprised/sad/angry）を被覆。"
  {2 {"normal" 2 "happy" 0 "surprised" 6 "sad" 36 "angry" 6}
   3 {"normal" 3 "happy" 1 "surprised" 7 "sad" 76 "angry" 7}})

(def style->speaker-name
  {2 "四国めたん" 0 "四国めたん" 6 "四国めたん" 36 "四国めたん"
   3 "ずんだもん" 1 "ずんだもん" 7 "ずんだもん" 76 "ずんだもん"})

(defn resolve-style-id
  "speaker(\"left\"/\"right\") + emotion → style_id。
   emotion-style? が true のときだけ emotion で切替（既定 false＝声色安定）。"
  ([speaker emotion] (resolve-style-id speaker emotion false))
  ([speaker emotion emotion-style?]
   (let [base (if (= speaker "left") speaker-left-default speaker-right-default)]
     (if-not emotion-style?
       base
       (get (get emotion-style base) (or emotion "normal") base)))))

(defn line-plan
  "1 line → VOICEVOX synthesis job 記述（IO なし）。
   :exec は :audio-query-url へ POST → 返った AudioQuery に :query-overrides を
   適用 → :synthesis-url へ POST、の 2 段で実行する（yukkuri と同じ contract）。
   空 text は {:ok false} を返す（例外を投げない — runtime 方針）。"
  [{:keys [speaker text emotion]} {:keys [engine-url speed pitch emotion-style?]
                                   :or {engine-url "http://127.0.0.1:50021"
                                        speed 1.0 pitch 0.0}}]
  (if (str/blank? (str text))
    {:ok false :error :empty-text}
    (let [sid (resolve-style-id speaker emotion emotion-style?)]
      {:ok true
       :engine :voicevox
       :speaker sid
       :speaker-name (get style->speaker-name sid)
       :text text
       :audio-query-url (str engine-url "/audio_query")
       :synthesis-url (str engine-url "/synthesis")
       :query-overrides (cond-> {}
                          (not= speed 1.0) (assoc :speedScale speed)
                          (not= pitch 0.0) (assoc :pitchScale pitch))})))

(defn plan-script
  "展開済み script（{:scenes [{:lines [...]}]}）→ per-line plan の平坦なベクタ。
   opts は line-plan に渡す（engine-url / speed / pitch / emotion-style?）。"
  ([script] (plan-script script {}))
  ([script opts]
   (vec
    (for [scene (:scenes script)
          line (:lines scene)]
      (line-plan line opts)))))

(defn speakers-used
  "plan 群から重複を除いた話者名（登場順）。"
  [plans]
  (reduce (fn [acc {:keys [speaker-name]}]
            (if (and speaker-name (not (some #{speaker-name} acc)))
              (conj acc speaker-name)
              acc))
          []
          (filter :ok plans)))

(defn credit-lines
  "\"VOICEVOX:四国めたん\" 形のクレジット行（商用必須）。"
  [plans]
  (mapv #(str "VOICEVOX:" %) (speakers-used plans)))

(defn credit-string
  "description に差し込む 1 行クレジット（factcheck metadata チェックを満たす）。"
  [plans]
  (str/join " / " (credit-lines plans)))
