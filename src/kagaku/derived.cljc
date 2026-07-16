(ns kagaku.derived
  "派生 claim の決定論評価。claim から claim を導く算術（跳躍高 ÷ 体長 =
   体長の何倍跳ぶか、等）を LLM でなくここで計算する — 科学解説の花形の
   数字（『体長の◯倍』『秒速◯m』）を、なお LLM 非算術のまま台本に出す経路。

   :derived source 形:
   {:kind :derived
    :op   :ratio | :product | :sum | :diff | :scale
    :from [claim-id ...]   ; :scale は [claim-id] + :by 定数
    :by   n}               ; :scale のみ

   評価規則（単位の扱い）:
   - :ratio  同次元 2 claim → dimensionless（大きい単位に揃えて割る）。
             例: 20cm ÷ 2mm = 100 ratio。
   - :sum/:diff 同次元 2+ claim → その次元（第 1 claim の単位に揃える）。
   - :product 2 claim → 次元は現状 dimensionless 同士のみ許可（面積・体積の
             一般積は単位代数が要るので範囲外。必要になったら拡張）。
   - :scale  1 claim × 無次元定数 :by → 同単位。

   出力の :quantity を、その :derived claim 自身の宣言 :value と
   照合するのは factcheck 側（ここは計算に徹する）。"
  (:require [kagaku.units :as units]))

(defn- vals-of [from claims-by-id]
  (map #(get-in claims-by-id [% :value]) from))

(defn evaluate
  "1 つの :derived claim を評価して {:ok? :value | :error} を返す。
   claims-by-id は id → claim（:value を持つもの）。"
  [{:keys [op from by]} claims-by-id]
  (let [vs (vals-of from claims-by-id)]
    (cond
      (some nil? vs)
      {:ok? false :error :from-claim-missing :from from}

      (not (every? #(units/known? (:unit %)) vs))
      {:ok? false :error :unknown-unit :units (map :unit vs)}

      :else
      (case op
        :ratio
        (if (and (= 2 (count vs)) (units/same-dimension? (:unit (first vs))
                                                         (:unit (second vs))))
          {:ok? true
           :value {:quantity (/ (units/to-base (first vs))
                                (units/to-base (second vs)))
                   :unit "ratio"}}
          {:ok? false :error :ratio-needs-two-same-dimension :units (map :unit vs)})

        (:sum :diff)
        (let [target (:unit (first vs))]
          (if (every? #(units/same-dimension? (:unit %) target) vs)
            (let [qs (map #(:quantity (units/convert % target)) vs)]
              {:ok? true
               :value {:quantity (if (= op :sum)
                                   (reduce + qs)
                                   (reduce - qs))
                       :unit target}})
            {:ok? false :error :mixed-dimensions :units (map :unit vs)}))

        :product
        (if (and (= 2 (count vs))
                 (every? #(= "ratio" (:unit %)) vs))
          {:ok? true :value {:quantity (* (:quantity (first vs))
                                          (:quantity (second vs)))
                             :unit "ratio"}}
          {:ok? false :error :product-only-dimensionless :units (map :unit vs)})

        :scale
        (if (and (= 1 (count vs)) (number? by))
          {:ok? true :value {:quantity (* (:quantity (first vs)) by)
                             :unit (:unit (first vs))}}
          {:ok? false :error :scale-needs-one-claim-and-by})

        {:ok? false :error :unknown-op :op op}))))

(def rel-tolerance 0.02)

(defn check
  "episode の全 :derived claim を評価し、宣言 :value と照合する。
   {:ok? bool :failed [{:claim :reason :expected :computed}]} を返す。"
  [claims]
  (let [by-id (into {} (map (juxt :id identity) (filter :value claims)))
        derived (filter #(= :derived (get-in % [:source :kind])) claims)
        failed
        (vec
         (for [{:keys [id value source]} derived
               :let [r (evaluate source by-id)]
               :when (or (not (:ok? r))
                         (not= (:unit value) (get-in r [:value :unit]))
                         (let [c (get-in r [:value :quantity])
                               e (:quantity value)]
                           (or (zero? c)
                               (> (/ (abs (- e c)) (abs c)) rel-tolerance))))]
           {:claim id
            :reason (if (:ok? r) :value-or-unit-mismatch (:error r))
            :expected value :computed (:value r)}))]
    {:ok? (empty? failed) :failed failed}))
