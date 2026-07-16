(ns kagaku.simcase
  "sim case EDN の検証と、実行結果 → claim の束縛・整合チェック。
   純データ、外部 IO なし — solver の実行そのものは :exec 側
   （kotoba-lang の cae.solver dispatch / nagare / kudaki / num）が担い、
   ここでは case の妥当性・結果と台本主張の一致・ベンチマーク datom 化だけを行う。

   case 形:
   {:id      kw
    :solver  {:kind kw}                ; 下記 solver-kinds のいずれか
    :domain  map                       ; 形状・境界条件・初期条件（solver 依存）
    :budget  {:cells n :steps n}       ; 実行規模の上限宣言（性能ベンチの計画値）
    :outputs {output-kw {:unit str}}}  ; この case が生む出力とその単位

   実行結果（:exec 側が返す）:
   {case-id {:outputs {output-kw {:quantity n :unit str}}
             :bench   {:wall-ms n :cells n :steps n :runtime kw}}}"
  )

(def solver-kinds
  "エピソードが使ってよい solver kind。実体はすべて kotoba-lang sim スタック —
   このチャンネル自体が sim スタックの性能・適用範囲を実地で試すベンチ台を兼ねる。"
  #{:fvm-simple          ; nagare — 有限体積 非圧縮 NS（SIMPLE/PISO）
    :lbm                 ; cae-solver contract の :lbm（host adapter 経由）
    :explicit-dynamics   ; kudaki — 陽解法 構造動力学（衝突・落下・成形）
    :linear-static       ; fea — 線形静解析
    :reduced-order-aero  ; kami-engine-aero — 空力 Cd 分解
    :rom-fc              ; kami-engine-echem — PEM 燃料電池 ROM
    :road-load           ; kami-engine-vphysics — 走行抵抗・エネルギー
    :two-body-orbit      ; 二体軌道（reduced-order。月接近 what-if 用）
    :tidal-scaling       ; 潮汐力 1/d^3 スケーリング（reduced-order）
    :scaling-law         ; アロメトリー / 相似則（動物比較用）
    :numeric-experiment});; num ベースの決定論数値実験（数学・物理 3 分解説用）

(def ^:const max-cells
  "1 episode の 1 case が宣言してよいメッシュ規模上限。renderer/solve の
   無人運転で wall-clock が発散しないための計画値ガード（実測は bench 側）。"
  2000000)

(defn validate-case
  [{:keys [id solver outputs budget] :as _case}]
  (let [errors
        (cond-> []
          (not (keyword? id)) (conj [:case/id-missing id])
          (not (contains? solver-kinds (:kind solver)))
          (conj [:case/solver-unknown id (:kind solver)])
          (or (empty? outputs)
              (not (every? (fn [[k v]] (and (keyword? k) (string? (:unit v))))
                           outputs)))
          (conj [:case/outputs-malformed id])
          (and (:cells budget) (> (:cells budget) max-cells))
          (conj [:case/budget-exceeded id (:cells budget)]))]
    {:valid? (empty? errors) :errors errors}))

(defn validate-cases [cases]
  (let [results (map validate-case cases)
        errors (into (vec (mapcat :errors results))
                     (let [ids (map :id cases)]
                       (when (not= (count ids) (count (set ids)))
                         [[:cases/duplicate-ids]])))]
    {:valid? (empty? errors) :errors errors}))

(def ^:const default-rel-tolerance 0.02)

(defn claim-consistency
  "sim 出所の claim と実行結果の一致を検査する。
   台本の数値は sim 結果から**転記**される建前なので、相対誤差 tol
   （既定 2%。丸め・有効数字ぶんの遊びだけ許す）を超えたら fail。
   単位文字列は完全一致を要求する（次元換算を暗黙にやらない）。"
  ([claims results] (claim-consistency claims results default-rel-tolerance))
  ([claims results tol]
   (vec
    (for [{:keys [id value source]} claims
          :when (= :sim (:kind source))]
      (let [{:keys [case output]} source
            actual (get-in results [case :outputs output])]
        (cond
          (nil? actual)
          {:claim id :ok? false :reason :result-missing
           :expected value :actual nil}

          (not= (:unit value) (:unit actual))
          {:claim id :ok? false :reason :unit-mismatch
           :expected value :actual actual}

          (let [a (:quantity actual) c (:quantity value)]
            (or (zero? a)
                (> (/ (abs (- c a)) (abs a)) tol)))
          {:claim id :ok? false :reason :value-mismatch
           :expected value :actual actual}

          :else
          {:claim id :ok? true :expected value :actual actual}))))))

(defn bench-datoms
  "実行結果の :bench 計測を append-only 台帳（docs/sim-benchmark-ledger.edn）
   行に変換する。kotoba-lang sim スタックの性能トラッキングへの還元経路。"
  [episode-id cases results]
  (vec
   (for [{:keys [id solver]} cases
         :let [bench (get-in results [id :bench])]
         :when bench]
     (merge {:kagaku.bench/episode episode-id
             :kagaku.bench/case id
             :kagaku.bench/solver (:kind solver)}
            (update-keys bench #(keyword "kagaku.bench" (name %)))))))
