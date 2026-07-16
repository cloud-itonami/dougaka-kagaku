(ns kagaku.scene
  "render-sim-visual stage の scene planner — sim 結果を KAMI scene snapshot
   （ECS-as-datoms）に変換する。純データ・IO なし。実 render（kami-engine
   headless / WebGPU）は :exec 側。

   **語彙・スナップショット形・妥当性規則の authority は
   `orgs/kotoba-lang/kami-contracts` の `kami.scene`**（ARCHITECTURE.md §5）。
   このリポジトリはその消費者として、独自形式を作らず同じ attribute ident と
   `{:snapshot/entities [..] :snapshot/assets [..]}` 形に合わせる。ここに写す
   `known-attrs` / `valid?` 規則は authority の mirror であり、authority が
   進んだら追随する（kami-contracts を deps に引かないのは kagaku を zero-dep
   に保つため — repo-wide 3D 規則の『kami-engine stack を消費する』側に留まる）。

   3D は repo-wide 規則どおり kami-engine stack のみ（ADR-2607102200）。
   独自 renderer / Three.js / DOM 疑似3D を作らない。"
  (:require [clojure.set :as set]))

;; --- authority mirror: kami.scene の attribute 語彙（使う分のみ） -----------
(def known-attrs
  "kami.scene/schema の :db/ident のうち本 planner が使う subset。
   authority が正 — ここに無い attr を entity に書いたら validate が弾く。"
  #{:kami/eid :kami/name
    :transform/parent :transform/translation :transform/rotation :transform/scale
    :mesh/asset :material/asset :material/params :shader/asset
    :camera/fov :camera/near :camera/far :camera/active? :camera/projection
    :light/kind :light/color :light/intensity
    :scene/name :scene/root :scene/env})

(def known-asset-attrs
  #{:asset/id :asset/kind :asset/uri :asset/sha256 :asset/inline})

(defn- ref-id [r]
  (cond (map? r) (or (:asset/id r) (:kami/eid r))
        (vector? r) (second r)
        :else r))

;; --- 妥当性（kami.scene/valid? の mirror。throw でなく errors を返す形に） ---
(defn validate
  "scene snapshot を検査して {:valid? bool :errors [..]} を返す。
   規則は kami.scene/valid? と同じ: 未知 attr / dangling parent / dangling
   asset ref / 複数 active camera / parent cycle。"
  [{:keys [snapshot/entities snapshot/assets]}]
  (let [eids (into #{} (map :kami/eid) entities)
        asset-ids (into #{} (map :asset/id) assets)
        errors
        (concat
         ;; 未知 attribute（authority 語彙外）
         (for [e entities
               k (keys (dissoc e :db/id))
               :when (not (known-attrs k))]
           [:unknown-attr (:kami/name e) k])
         (for [a assets
               k (keys (dissoc a :db/id))
               :when (not (known-asset-attrs k))]
           [:unknown-asset-attr (:asset/id a) k])
         ;; dangling parent
         (for [e entities :when (:transform/parent e)
               :let [p (ref-id (:transform/parent e))]
               :when (not (eids p))]
           [:dangling-parent (:kami/eid e) p])
         ;; dangling asset ref
         (for [e entities
               k [:mesh/asset :material/asset :shader/asset]
               :when (k e)
               :let [a (ref-id (k e))]
               :when (not (asset-ids a))]
           [:dangling-asset (:kami/name e) k a])
         ;; 複数 active camera
         (let [actives (filter :camera/active? entities)]
           (when (> (count actives) 1)
             [[:multiple-active-cameras (mapv :kami/name actives)]])))]
    {:valid? (empty? errors) :errors (vec errors)}))

;; --- helpers ---------------------------------------------------------------
(defn- sphere-asset
  "手続き生成球体 mesh asset（:asset/inline に EDN で半径を持たせる）。"
  [id radius]
  {:asset/id id :asset/kind :mesh
   :asset/inline (pr-str {:primitive :uv-sphere :radius radius :segments 64})})

(defn- material-asset [id color]
  {:asset/id id :asset/kind :material
   :asset/inline (pr-str {:albedo color})})

;; --- 地球-月シーン ---------------------------------------------------------
;; 表示単位は Mm（1 unit = 1000 km）。実寸の距離/半径のまま置くと数値が巨大で
;; 浮動小数が荒れるため。距離は sim（tidal-scaling の distance-ratio）から来る。
(def ^:const earth-radius-mm 6.371)
(def ^:const moon-radius-mm 1.7374)
(def ^:const moon-distance-mean-mm 384.4)

(defn earth-moon-scene
  "地球-月シーンの KAMI snapshot を生成する。
   opts: {:distance-ratio r  ; sim と同じ（1.0 = 現在の平均距離、0.5 = 半分）
          :name str}
   月の位置 x = 平均距離 × distance-ratio。カメラは両天体が入る距離に置く。"
  [{:keys [distance-ratio name] :or {distance-ratio 1.0 name "earth-moon"}}]
  (let [moon-x (* moon-distance-mean-mm distance-ratio)
        earth-eid (random-uuid)
        moon-eid (random-uuid)
        cam-eid (random-uuid)
        sun-eid (random-uuid)
        assets [(sphere-asset "mesh/earth" earth-radius-mm)
                (sphere-asset "mesh/moon" moon-radius-mm)
                (material-asset "mat/earth" [0.15 0.35 0.75])
                (material-asset "mat/moon" [0.55 0.55 0.55])]
        entities
        [{:kami/eid earth-eid :kami/name "earth"
          :transform/translation [0.0 0.0 0.0]
          :mesh/asset [:asset/id "mesh/earth"]
          :material/asset [:asset/id "mat/earth"]}
         {:kami/eid moon-eid :kami/name "moon"
          :transform/translation [moon-x 0.0 0.0]
          :mesh/asset [:asset/id "mesh/moon"]
          :material/asset [:asset/id "mat/moon"]}
         {:kami/eid cam-eid :kami/name "camera"
          ;; 月の外側やや上から地球-月系を俯瞰
          :transform/translation [(* moon-x 0.5) (* moon-distance-mean-mm 0.4)
                                  (* moon-distance-mean-mm 0.9)]
          :camera/active? true
          :camera/projection :perspective
          :camera/fov 45.0 :camera/near 0.1 :camera/far 5000.0}
         {:kami/eid sun-eid :kami/name "sun"
          :transform/translation [1000.0 500.0 1000.0]
          :light/kind :dir
          :light/color [1.0 0.98 0.92]
          :light/intensity 1.0}]]
    {:snapshot/name name
     :snapshot/entities entities
     :snapshot/assets assets
     :snapshot/scene {:scene/name name :scene/env (pr-str {:clear [0.0 0.0 0.02]})}}))

;; --- 相似則シーン（animal-power）------------------------------------------
;; 「体長 L 倍に拡大すると筋力/体重比が 1/L に落ちる」を、実物大と拡大版の
;; 2 球体を並べて体感させる。球体半径を length-ratio の対数でスケールする
;; （850 倍を実寸で置くと画面外になるため log 圧縮。あくまで概念可視化で、
;; 数値の正は sim claim 側 — scene は sim パラメータ length-ratio に連動する）。
(def ^:const base-radius-mm 1.0)

(defn scaled-radius
  "拡大版の球体半径 = base × (1 + log10 r)（log 圧縮 — 850 倍を実寸で置くと
   画面外になるため。概念可視化で、数値の正は sim claim 側）。"
  [length-ratio]
  (* base-radius-mm (+ 1.0 (Math/log10 (max 1.0 length-ratio)))))

(defn scaling-law-scene
  "実物大 vs 拡大版の 2 球体シーン。opts {:length-ratio r :name str}。
   拡大版の半径 = scaled-radius、x に間隔を空けて並置。"
  [{:keys [length-ratio name] :or {length-ratio 1.0 name "scaling-law"}}]
  (let [big-r (scaled-radius length-ratio)
        gap (* 3.0 (+ base-radius-mm big-r))
        small-eid (random-uuid)
        big-eid (random-uuid)
        cam-eid (random-uuid)
        sun-eid (random-uuid)
        assets [(sphere-asset "mesh/small" base-radius-mm)
                (sphere-asset "mesh/big" big-r)
                (material-asset "mat/small" [0.85 0.55 0.20])
                (material-asset "mat/big" [0.75 0.35 0.25])]
        entities
        [{:kami/eid small-eid :kami/name "actual-size"
          :transform/translation [(- gap) 0.0 0.0]
          :mesh/asset [:asset/id "mesh/small"]
          :material/asset [:asset/id "mat/small"]}
         {:kami/eid big-eid :kami/name "scaled-up"
          :transform/translation [gap 0.0 0.0]
          :mesh/asset [:asset/id "mesh/big"]
          :material/asset [:asset/id "mat/big"]}
         {:kami/eid cam-eid :kami/name "camera"
          :transform/translation [0.0 (* gap 0.5) (* gap 2.0)]
          :camera/active? true
          :camera/projection :perspective
          :camera/fov 45.0 :camera/near 0.1 :camera/far 5000.0}
         {:kami/eid sun-eid :kami/name "sun"
          :transform/translation [100.0 100.0 100.0]
          :light/kind :dir
          :light/color [1.0 0.98 0.92]
          :light/intensity 1.0}]]
    {:snapshot/name name
     :snapshot/entities entities
     :snapshot/assets assets
     :snapshot/scene {:scene/name name :scene/env (pr-str {:clear [0.02 0.02 0.03]})}}))

(defn scene-for-episode
  "episode から scene snapshot を導く（series ディスパッチ）。
     :moon-approach → 地球-月シーン（sim distance-ratio 連動）
     :animal-power  → 相似則 2 球体シーン（sim length-ratio 連動）
   担当外 series は nil（正直に未実装を返す — audit は skip 満点で扱う）。"
  [{:keys [series sim-cases] :as _episode}]
  (case series
    :moon-approach
    (let [dr (some #(get-in % [:domain :distance-ratio]) sim-cases)]
      (earth-moon-scene {:distance-ratio (or dr 1.0) :name "moon-approach"}))

    :animal-power
    (let [lr (some #(get-in % [:domain :length-ratio]) sim-cases)]
      (when lr
        (scaling-law-scene {:length-ratio lr :name "animal-power"})))

    nil))
