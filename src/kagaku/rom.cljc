(ns kagaku.rom
  "reduced-order model solver — 純計算・外部 IO なし（IO を持たない solver は
   pure-planner 境界に反しない）。cae.solver contract と同型の
   [:solver :kind] dispatch。高忠実度 kind（:fvm-simple / :lbm /
   :explicit-dynamics / :linear-static）はここに実装せず :exec 側の
   cae.solver hosts（nagare / kudaki / fea）に委ねる — ここは episode の
   what-if / スケーリング系 reduced-order だけを持つ。

   solve: (solve case constants) → {:outputs {output-kw {:quantity n :unit str}}}
   定数は resources/constants.edn 由来を呼び出し側が渡す（このnsに数値を
   ハードコードしない — 定数の出典一元化を solver 側でも守る）。"
  )

(defn- constant
  "constants テーブルから値を引く。無ければ例外 — 出典なしの数値を
   solver が勝手に補わない。"
  [constants k expected-unit]
  (let [{:keys [quantity unit] :as c} (get constants k)]
    (when (nil? c)
      (throw (ex-info "constant not found" {:ref k})))
    (when (not= unit expected-unit)
      (throw (ex-info "constant unit mismatch"
                      {:ref k :expected expected-unit :actual unit})))
    quantity))

(defmulti solve
  "case EDN を解いて outputs を返す。決定論・純関数。"
  (fn [case _constants] (get-in case [:solver :kind])))

(defmethod solve :tidal-scaling
  ;; 潮汐力 ∝ 1/d^3。distance-ratio r に対する現在比 = r^-3。
  [{:keys [domain]} _constants]
  (let [r (:distance-ratio domain)]
    (when-not (and (number? r) (pos? r))
      (throw (ex-info "tidal-scaling: :distance-ratio must be positive"
                      {:domain domain})))
    {:outputs {:tidal-force-ratio
               {:quantity (/ 1.0 (* r r r)) :unit "ratio"}}}))

(defmethod solve :two-body-orbit
  ;; ケプラー第三法則: T = 2π sqrt(a^3 / (G (m1 + m2)))。
  ;; :domain の :primary / :secondary は constants テーブルの質量 ref。
  [{:keys [domain]} constants]
  (let [a-m (* (:semi-major-axis-km domain) 1000.0)
        g (constant constants :gravitational-constant "m^3 kg^-1 s^-2")
        m1 (constant constants (:primary domain) "kg")
        m2 (constant constants (:secondary domain) "kg")
        mu (* g (+ m1 m2))
        t-s (* 2.0 Math/PI (Math/sqrt (/ (* a-m a-m a-m) mu)))]
    {:outputs {:orbital-period-days {:quantity (/ t-s 86400.0) :unit "day"}
               :orbital-period-s {:quantity t-s :unit "s"}}}))

(defmethod solve :scaling-law
  ;; 相似則: 長さ比 L に対し、断面積(筋力・強度) ∝ L^2、体積(質量) ∝ L^3。
  ;; 「ノミが人間サイズなら」の誤解の核心 — 筋力/体重比は L^-1 で悪化する。
  [{:keys [domain]} _constants]
  (let [l (:length-ratio domain)]
    (when-not (and (number? l) (pos? l))
      (throw (ex-info "scaling-law: :length-ratio must be positive"
                      {:domain domain})))
    {:outputs {;; 入力のエコー。台本が体長比そのものに言及するとき、この出力に
               ;; 束縛させれば domain を変えたのに台本の数字が古いままという
               ;; 不整合を claim-consistency が検出できる。
               :length-ratio {:quantity l :unit "ratio"}
               :area-ratio {:quantity (* l l) :unit "ratio"}
               :mass-ratio {:quantity (* l l l) :unit "ratio"}
               :strength-to-weight-ratio {:quantity (/ 1.0 l) :unit "ratio"}
               ;; 語り向けの逆数表現（「◯倍の不利」= 筋力/体重比が 1/◯）。
               ;; strength-to-weight-ratio と同じ物理の別表示。
               :strength-to-weight-penalty {:quantity l :unit "ratio"}}}))

(defmethod solve :numeric-experiment
  ;; 決定論数値実験。:experiment で分岐（乱数を使う場合も seed 必須の
  ;; 決定論 PRNG — 同じ case は常に同じ出力）。
  [{:keys [domain]} _constants]
  (case (:experiment domain)
    :fold-to-moon
    ;; 厚さ t0 の紙を n 回折る → t0 * 2^n
    (let [{:keys [thickness-mm folds]} domain
          thickness-km (* thickness-mm 1.0E-6 (Math/pow 2.0 folds))]
      {:outputs {:final-thickness-km {:quantity thickness-km :unit "km"}
                 ;; 入力エコー（台本が「0.1mm」「42回」に言及するときの束縛先）。
                 :initial-thickness-mm {:quantity (double thickness-mm) :unit "mm"}
                 :fold-count {:quantity (double folds) :unit "count"}}})

    :pi-monte-carlo
    ;; 決定論 LCG（seed 必須）で 1/4 円ヒット率から π を推定。
    (let [{:keys [seed samples]} domain
          _ (when-not (and (int? seed) (pos-int? samples))
              (throw (ex-info "pi-monte-carlo: :seed and :samples required"
                              {:domain domain})))
          m 2147483647.0
          hits (loop [i 0 s (double seed) hits 0]
                 (if (= i samples)
                   hits
                   (let [s1 (mod (* 48271.0 s) m)
                         s2 (mod (* 48271.0 s1) m)
                         x (/ s1 m) y (/ s2 m)]
                     (recur (inc i) s2
                            (if (<= (+ (* x x) (* y y)) 1.0)
                              (inc hits) hits)))))
          estimate (* 4.0 (/ hits (double samples)))]
      {:outputs {;; 円周率は倍率でなく純粋な無次元数 → unit "1"（バレ数表示）。
                 :pi-estimate {:quantity estimate :unit "1"}
                 ;; sample 数のエコー（台本が「10万個」に言及するときの束縛先）。
                 :sample-count {:quantity (double samples) :unit "count"}
                 ;; 真の π（数学定数、solver が決定論計算してよい）との絶対誤差。
                 :abs-error {:quantity (abs (- estimate Math/PI)) :unit "1"}}})

    (throw (ex-info "numeric-experiment: unknown :experiment"
                    {:experiment (:experiment domain)}))))

(defmethod solve :default
  ;; 高忠実度 kind は :exec 側（cae.solver hosts）へ。ここで解けない kind を
  ;; 黙って近似しない。
  [case _constants]
  (throw (ex-info "solver kind not available in kagaku.rom (delegate to :exec cae.solver host)"
                  {:kind (get-in case [:solver :kind])})))

(def rom-kinds
  "kagaku.rom がローカルに解ける kind（それ以外は :exec 委譲）。"
  #{:tidal-scaling :two-body-orbit :scaling-law :numeric-experiment})

(defn run-cases
  "episode の sim-cases のうち rom で解けるものを全て解き、
   kagaku.simcase の results 形（bench 抜き — 計時は runner 側）で返す。"
  [cases constants]
  (into {}
        (for [{:keys [id] :as c} cases
              :when (contains? rom-kinds (get-in c [:solver :kind]))]
          [id (solve c constants)])))
