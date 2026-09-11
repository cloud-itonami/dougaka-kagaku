(ns kagaku.units
  "単位の次元テーブルと換算。純データ・純関数。

   claim の :unit は文字列で、sim 出力との比較は**完全一致**を要求する
   （kagaku.simcase/claim-consistency — 暗黙の次元換算をしない方針）。
   その方針は維持したまま、**:derived claim の評価に限って**明示的な換算を
   許す（20cm ÷ 2mm のような派生比は換算なしには計算できないため）。

   ここに無い単位は換算対象外 — 黙って 1.0 と見なさず nil を返し、
   呼び出し側が reject する。")

(def table
  "unit → [dimension base への換算係数]。base は SI 基本（m / s / kg）。"
  {"mm" [:length 0.001]
   "cm" [:length 0.01]
   "m" [:length 1.0]
   "km" [:length 1000.0]
   "s" [:time 1.0]
   "min" [:time 60.0]
   "h" [:time 3600.0]
   "day" [:time 86400.0]
   "year" [:time 31557600.0]          ; ユリウス年（365.25 日）
   "g" [:mass 0.001]
   "kg" [:mass 1.0]
   "m/s" [:speed 1.0]
   "km/h" [:speed 0.2777777777777778]
   "N" [:force 1.0]
   "kN" [:force 1000.0]
   "kW" [:power 1000.0]
   "W" [:power 1.0]
   "ratio" [:dimensionless 1.0]    ; 倍率（8倍・850倍）
   "1" [:dimensionless 1.0]        ; 純粋な無次元数（円周率・誤差、バレ数）
   "count" [:count 1.0]            ; 個数（10万個）
   "rad" [:angle 1.0]              ; 角度（ラジアン、二重振り子の隔たり）
   "Hz" [:frequency 1.0]})         ; 周波数（笛の音）

(defn dimension [unit] (first (get table unit)))
(defn factor [unit] (second (get table unit)))
(defn known? [unit] (contains? table unit))

(defn to-base
  "quantity を base 単位に換算する。未知単位は nil。"
  [{:keys [quantity unit]}]
  (when-let [f (factor unit)]
    (* quantity f)))

(defn same-dimension?
  [unit-a unit-b]
  (boolean (and (known? unit-a) (known? unit-b)
                (= (dimension unit-a) (dimension unit-b)))))

(defn convert
  "value を target-unit に換算する。次元が違う / 未知単位なら nil。"
  [{:keys [unit] :as value} target-unit]
  (when (same-dimension? unit target-unit)
    {:quantity (/ (to-base value) (factor target-unit))
     :unit target-unit}))
