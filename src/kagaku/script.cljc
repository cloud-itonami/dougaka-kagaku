(ns kagaku.script
  "L/R 掛け合い台本の planner — yukkuri generate_script のスキーマ
   {:scenes [{:location :action :lines [{:speaker :text :emotion}]}]} を継承し、
   数値主張の provenance 埋め込み規約を追加する。純データ、外部 IO なし。

   規約（LLM 非算術の台本側の強制）:
   - line の生 :text に **アラビア数字を直書きしてはならない**（数量でない
     語呂も含めて全面禁止 — 『ひとつ』等の和語で書く）。
   - 数値は placeholder `{{claim-id}}` でのみ言及でき、expand-script が
     episode の（factcheck 済み）claim 値から**機械的に転記**する。
     LLM は数値を一度も書かない — 書けるのは claim への参照だけ。
   - episode の数値 claim はすべて台本のどこかで言及されなければならない
     （言及されない数値主張は description/telop にも出せない = 死に claim）。"
  (:require [kotoba.lang.text :as str]))

(def speakers #{"left" "right"})
(def emotions #{"normal" "happy" "surprised" "sad" "angry"})

(def placeholder-re #"\{\{([a-zA-Z0-9\-]+)\}\}")

(defn placeholder-ids
  "text 中の {{claim-id}} を keyword の集合で返す。"
  [text]
  (set (map (comp keyword second) (re-seq placeholder-re (str text)))))

(defn- strip-placeholders [text]
  (str/replace (str text) placeholder-re ""))

(defn- line-errors [scene-i line-j {:keys [speaker text emotion]} claim-ids]
  (let [loc {:scene scene-i :line line-j}]
    (cond-> []
      (not (contains? speakers speaker))
      (conj [:line/speaker-invalid loc speaker])

      (str/blank? (str text))
      (conj [:line/text-missing loc])

      (and emotion (not (contains? emotions emotion)))
      (conj [:line/emotion-invalid loc emotion])

      (re-find #"[0-9０-９]" (strip-placeholders text))
      (conj [:line/raw-digits-forbidden loc])

      :always
      (into (for [pid (placeholder-ids text)
                  :when (not (contains? claim-ids pid))]
              [:line/claim-unknown loc pid])))))

(defn validate
  "script を episode claims と突き合わせて検証する。
   claims は kagaku.scenario の :claims（:value を持つものが数値 claim）。"
  [{:keys [scenes] :as _script} claims]
  (let [claim-ids (set (map :id claims))
        numeric-ids (set (map :id (filter :value claims)))
        spoken (reduce into #{}
                       (for [scene scenes, line (:lines scene)]
                         (placeholder-ids (:text line))))
        errors
        (-> []
            (cond->
              (empty? scenes) (conj [:script/scenes-empty]))
            (into (apply concat
                         (map-indexed
                          (fn [i scene]
                            (if (empty? (:lines scene))
                              [[:scene/lines-empty {:scene i}]]
                              (apply concat
                                     (map-indexed
                                      (fn [j line] (line-errors i j line claim-ids))
                                      (:lines scene)))))
                          scenes)))
            (into (for [cid (sort (remove spoken numeric-ids))]
                    [:claims/unspoken cid])))]
    {:valid? (empty? errors) :errors errors}))

;; --- 転記（機械的展開） ---------------------------------------------------

(def unit-ja
  "表示単位の和訳。無い unit はそのまま後置する。
   \"1\"（純粋無次元数）と \"count\"（個数）は接尾辞なし＝バレ数表示
   （周辺テキストが『個』等を付ける。円周率に『倍』を付けない）。"
  {"ratio" "倍" "day" "日" "s" "秒" "km" "km" "m" "m" "km/h" "km/h"
   "1" "" "count" "" "rad" "ラジアン"})

(defn fmt-value
  "claim :value → 表示文字列。整数は整数表記、それ以外は claim の値を
   そのまま（claim 値が SSoT — ここで丸め直さない。丸めたければ claim 値
   自体を有効数字で書き、sim との差は factcheck の ≤2% 許容が吸収する）。"
  [{:keys [quantity unit]}]
  (let [q (if (== quantity (Math/floor quantity)) (long quantity) quantity)]
    (str q (get unit-ja unit unit))))

(defn expand-line
  "line :text の {{claim-id}} を claim 値の表示文字列に置換する。"
  [text claims-by-id]
  (str/replace (str text) placeholder-re
               (fn [[whole cid]]
                 (if-let [c (get claims-by-id (keyword cid))]
                   (fmt-value (:value c))
                   whole))))

(defn expand
  "script 全体を展開する（TTS / telop に渡す最終テキスト）。
   validate を通した script にだけ使うこと（未知 claim は素通しで残る）。"
  [script claims]
  (let [by-id (into {} (map (juxt :id identity) (filter :value claims)))]
    (update script :scenes
            (fn [scenes]
              (mapv (fn [scene]
                      (update scene :lines
                              (fn [lines]
                                (mapv (fn [line]
                                        (update line :text expand-line by-id))
                                      lines))))
                    scenes)))))

;; --- LLM request spec（pure-planner: 実呼び出しは :exec 側） ---------------

(defn request-spec
  "generate-script stage の LLM request spec。yukkuri と同じ
   {:system :user :model-hint} 形。数値の直書き禁止と placeholder 規約を
   プロンプトで指示する（違反は validate が機械的に弾く）。"
  [{:keys [question claims beats] :as _episode}]
  {:model-hint :murakumo-text
   :system
   (str "あなたは科学解説チャンネルの放送作家。左（解説役・落ち着いた口調）と"
        "右（聞き役・驚き役）の掛け合い台本を JSON で書く。"
        "形式: {\"scenes\":[{\"location\":str,\"action\":str,"
        "\"lines\":[{\"speaker\":\"left\"|\"right\",\"text\":str,"
        "\"emotion\":\"normal\"|\"happy\"|\"surprised\"|\"sad\"|\"angry\"}]}]}。"
        "【最重要】数値をアラビア数字で直接書くことを禁止する。数値に言及する"
        "ときは必ず {{claim-id}} placeholder を使う（値はシステムが検証済み"
        "データから転記する）。使える claim-id と意味は user メッセージに列挙"
        "する。列挙にない数値の言及は和語（『およそ半分』等）でぼかすか避ける。"
        "すべての claim を少なくとも一度は台本中で言及すること。")
   :user
   (str "問い: " question "\n\n"
        "使える claim（{{id}} で参照）:\n"
        (str/join "\n"
                  (for [{:keys [id text]} (filter :value claims)]
                    (str "- {{" (name id) "}} … " text)))
        "\n\n展開ビート:\n"
        (str/join "\n" (map #(str "- " %) beats)))})
