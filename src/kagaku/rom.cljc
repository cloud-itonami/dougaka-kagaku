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

;; --- 二重振り子（決定論カオス）--------------------------------------------
;; m1=m2=1, L1=L2=1 の二重振り子。RK4 で積分し、初期値がわずかに違う 2 つの
;; 軌道が角空間でどれだけ発散するか（初期値鋭敏性＝カオス）を測る。g は定数。
(defn- dp-accel
  "θ1'' θ2''（標準閉形式、m1=m2=1, L1=L2=1）。"
  [t1 t2 w1 w2 g]
  (let [d (- t1 t2)
        cd (Math/cos d) sd (Math/sin d)
        den (- 2.0 (* cd cd))
        a1 (/ (- (* -1.0 sd (+ (* w1 w1 cd) (* w2 w2)))
                 (* g (- (* 2.0 (Math/sin t1)) (* (Math/sin t2) cd))))
              den)
        ;; ポテンシャル項は 2g(sinθ1 cosΔ − sinθ2)。Lagrangian からの導出で
        ;; sinθ2 に cosΔ は掛からない（この係数取り違えが energy drift の原因だった）。
        a2 (/ (+ (* sd (+ (* 2.0 w1 w1) (* w2 w2 cd)))
                 (* 2.0 g (- (* (Math/sin t1) cd) (Math/sin t2))))
              den)]
    [a1 a2]))

(defn- dp-step
  "RK4 1 ステップ。state=[t1 t2 w1 w2]。"
  [[t1 t2 w1 w2] g dt]
  (let [f (fn [[a b c e]]
            (let [[aa ab] (dp-accel a b c e g)] [c e aa ab]))
        add (fn [s k h] (mapv #(+ %1 (* h %2)) s k))
        k1 (f [t1 t2 w1 w2])
        k2 (f (add [t1 t2 w1 w2] k1 (/ dt 2.0)))
        k3 (f (add [t1 t2 w1 w2] k2 (/ dt 2.0)))
        k4 (f (add [t1 t2 w1 w2] k3 dt))]
    (mapv (fn [s a b c e] (+ s (* (/ dt 6.0) (+ a (* 2.0 b) (* 2.0 c) e))))
          [t1 t2 w1 w2] k1 k2 k3 k4)))

(defn- dp-energy
  "全エネルギー E=KE+PE（m=L=1、RK4 の妥当性検証用）。"
  [[t1 t2 w1 w2] g]
  (let [vx1 (* w1 (Math/cos t1)) vy1 (* w1 (Math/sin t1))
        vx2 (+ vx1 (* w2 (Math/cos t2))) vy2 (+ vy1 (* w2 (Math/sin t2)))
        y1 (- (Math/cos t1)) y2 (- y1 (Math/cos t2))
        ke (+ (* 0.5 (+ (* vx1 vx1) (* vy1 vy1)))
              (* 0.5 (+ (* vx2 vx2) (* vy2 vy2))))
        pe (+ (* g y1) (* g y2))]
    (+ ke pe)))

(defn- dp-run [state g dt steps]
  (loop [i 0 s state] (if (= i steps) s (recur (inc i) (dp-step s g dt)))))

(defmethod solve :numeric-experiment
  ;; 決定論数値実験。:experiment で分岐（乱数を使う場合も seed 必須の
  ;; 決定論 PRNG — 同じ case は常に同じ出力）。g は constants から取る。
  [{:keys [domain]} constants]
  (case (:experiment domain)
    :double-pendulum
    ;; 2 つの近い初期値の軌道を積分し、終端での角空間の隔たり（発散）を測る。
    (let [g (constant constants :standard-gravity "m/s^2")
          {:keys [theta1 theta2 epsilon steps dt]
           :or {theta1 2.0 theta2 2.0 epsilon 1.0e-3 steps 4000 dt 0.005}} domain
          s0 [theta1 theta2 0.0 0.0]
          s0b [(+ theta1 epsilon) theta2 0.0 0.0]
          e-start (dp-energy s0 g)
          end-a (dp-run s0 g dt steps)
          end-b (dp-run s0b g dt steps)
          e-end (dp-energy end-a g)
          dth1 (- (nth end-a 0) (nth end-b 0))
          dth2 (- (nth end-a 1) (nth end-b 1))
          divergence (Math/sqrt (+ (* dth1 dth1) (* dth2 dth2)))]
      {:outputs {:initial-gap {:quantity epsilon :unit "rad"}
                 :final-divergence {:quantity divergence :unit "rad"}
                 :sim-time-s {:quantity (* dt steps) :unit "s"}
                 ;; RK4 のエネルギー保存誤差（妥当性の自己申告、絵/数値には使わない）
                 :energy-drift {:quantity (Math/abs (- e-end e-start)) :unit "1"}}})
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

(defmethod solve :roche-limit
  ;; ロッシュ限界 = 潮汐力が衛星の自己重力を超えて衛星を引き裂く距離。
  ;;   剛体: d = R_p (2 ρ_p/ρ_s)^(1/3)、流体: d = 2.44 R_p (ρ_p/ρ_s)^(1/3)。
  ;; 密度は constants の質量・半径（球）から計算（ρ = M / (4/3 π R³)）— LLM が
  ;; 覚えた密度値でなく出典付き定数から導く（AI 非算術の徹底）。
  [{:keys [domain]} constants]
  (let [mp (constant constants (:primary-mass domain) "kg")
        rp-km (constant constants (:primary-radius domain) "km")
        ms (constant constants (:satellite-mass domain) "kg")
        rs-km (constant constants (:satellite-radius domain) "km")
        density (fn [m r-km]
                  (let [r (* r-km 1000.0)]
                    (/ m (* (/ 4.0 3.0) Math/PI r r r))))
        rho-p (density mp rp-km)
        rho-s (density ms rs-km)
        cube-root (fn [x] (Math/pow x (/ 1.0 3.0)))]
    {:outputs {:roche-rigid-km {:quantity (* rp-km (cube-root (* 2.0 (/ rho-p rho-s))))
                                :unit "km"}
               :roche-fluid-km {:quantity (* 2.44 rp-km (cube-root (/ rho-p rho-s)))
                                :unit "km"}
               :primary-density {:quantity rho-p :unit "kg/m^3"}
               :satellite-density {:quantity rho-s :unit "kg/m^3"}}}))

(defmethod solve :reduced-order-aero
  ;; 空力抗力 F = ½ ρ Cd A v²（抗力方程式）。空気密度 ρ は constants の
  ;; air-density-sea-level（出典 ISA、ρ0=1.225 kg/m³）× density-ratio。
  ;; density-ratio で真空チューブ（減圧）を表す — 抗力は ρ に線形なので、
  ;; 気圧を 1/N に薄めれば抗力も 1/N。Cd・前面積・速度は工学上の前提（domain 入力）。
  [{:keys [domain]} constants]
  (let [rho0 (constant constants :air-density-sea-level "kg/m^3")
        {:keys [cd frontal-area-m2 speed-mps air-density-ratio]
         :or {air-density-ratio 1.0}} domain]
    (when-not (and (number? cd) (number? frontal-area-m2) (number? speed-mps))
      (throw (ex-info "reduced-order-aero: :cd :frontal-area-m2 :speed-mps required"
                      {:domain domain})))
    (let [rho (* rho0 air-density-ratio)
          drag (* 0.5 rho cd frontal-area-m2 speed-mps speed-mps)]
      {:outputs {:drag-force-n {:quantity drag :unit "N"}
                 :drag-power-kw {:quantity (/ (* drag speed-mps) 1000.0) :unit "kW"}
                 ;; 入力エコー（台本の束縛先）
                 :speed-mps {:quantity (double speed-mps) :unit "m/s"}
                 :speed-kmh {:quantity (* 3.6 speed-mps) :unit "km/h"}
                 :density-ratio {:quantity (double air-density-ratio) :unit "ratio"}}})))

(defmethod solve :default
  ;; 高忠実度 kind は :exec 側（cae.solver hosts）へ。ここで解けない kind を
  ;; 黙って近似しない。
  [case _constants]
  (throw (ex-info "solver kind not available in kagaku.rom (delegate to :exec cae.solver host)"
                  {:kind (get-in case [:solver :kind])})))

(def rom-kinds
  "kagaku.rom がローカルに解ける kind（それ以外は :exec 委譲）。"
  #{:tidal-scaling :two-body-orbit :scaling-law :numeric-experiment
    :reduced-order-aero :roche-limit})

(defn run-cases
  "episode の sim-cases のうち rom で解けるものを全て解き、
   kagaku.simcase の results 形（bench 抜き — 計時は runner 側）で返す。"
  [cases constants]
  (into {}
        (for [{:keys [id] :as c} cases
              :when (contains? rom-kinds (get-in c [:solver :kind]))]
          [id (solve c constants)])))
