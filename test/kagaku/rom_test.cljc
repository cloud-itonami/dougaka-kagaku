(ns kagaku.rom-test
  (:require [clojure.test :refer [deftest is testing]]
            [kagaku.rom :as rom]
            [kagaku.simcase]))

(def constants
  {:gravitational-constant {:quantity 6.67430e-11 :unit "m^3 kg^-1 s^-2" :source "CODATA 2022"}
   :earth-mass {:quantity 5.9722e24 :unit "kg" :source "NASA"}
   :moon-mass {:quantity 7.342e22 :unit "kg" :source "NASA"}})

(deftest tidal-scaling-half-distance
  (testing "距離 1/2 → 潮汐力 8 倍（逆 3 乗則）"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :tidal-scaling}
                                        :domain {:distance-ratio 0.5}}
                                       constants)]
      (is (< 7.999 (get-in outputs [:tidal-force-ratio :quantity]) 8.001)))))

(deftest two-body-orbit-current-moon
  (testing "現在の月（a=384400km）の公転周期 ≈ 27.3 日（恒星月）を再現"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :two-body-orbit}
                                        :domain {:semi-major-axis-km 384400.0
                                                 :primary :earth-mass
                                                 :secondary :moon-mass}}
                                       constants)
          days (get-in outputs [:orbital-period-days :quantity])]
      ;; 実測の恒星月 27.32 日。二体・円軌道近似なので 1% 以内で一致すれば正
      (is (< 27.0 days 27.6)))))

(deftest two-body-orbit-half-distance
  (testing "半分の距離での周期（content/tsuki-half-distance.edn の claim 値の根拠）"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :two-body-orbit}
                                        :domain {:semi-major-axis-km 192200.0
                                                 :primary :earth-mass
                                                 :secondary :moon-mass}}
                                       constants)
          days (get-in outputs [:orbital-period-days :quantity])]
      (is (< 9.5 days 9.8)))))

(deftest scaling-law-flea
  (testing "体長 100 倍 → 筋力/体重比は 1/100（『人間サイズのノミ』の誤解の核心）"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :scaling-law}
                                        :domain {:length-ratio 100.0}}
                                       constants)]
      (is (= 10000.0 (get-in outputs [:area-ratio :quantity])))
      (is (= 1000000.0 (get-in outputs [:mass-ratio :quantity])))
      (is (= 0.01 (get-in outputs [:strength-to-weight-ratio :quantity])))
      (is (= 100.0 (get-in outputs [:strength-to-weight-penalty :quantity])))))
  (testing ":length-ratio は入力のエコー — 台本が体長比に言及するとき束縛先になり、
            domain 変更に台本が追随していない不整合を claim-consistency が検出できる"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :scaling-law}
                                        :domain {:length-ratio 850.0}}
                                       constants)]
      (is (= 850.0 (get-in outputs [:length-ratio :quantity]))))))

(deftest stale-script-number-detected-via-length-ratio-echo
  (testing "domain を 850 → 500 に変えたのに claim が 850 のままなら弾かれる"
    (let [claim {:id :size-ratio :text "850倍に拡大"
                 :value {:quantity 850.0 :unit "ratio"}
                 :source {:kind :sim :case :flea-scaling :output :length-ratio}}
          results {:flea-scaling (rom/solve {:solver {:kind :scaling-law}
                                             :domain {:length-ratio 500.0}}
                                            constants)}
          [row] (kagaku.simcase/claim-consistency [claim] results)]
      (is (not (:ok? row)))
      (is (= :value-mismatch (:reason row))))))

(deftest fold-to-moon
  (testing "0.1mm の紙を 42 回折る → 約 44 万 km（月を超える）"
    (let [{:keys [outputs]} (rom/solve {:solver {:kind :numeric-experiment}
                                        :domain {:experiment :fold-to-moon
                                                 :thickness-mm 0.1 :folds 42}}
                                       constants)
          km (get-in outputs [:final-thickness-km :quantity])]
      (is (< 400000.0 km 480000.0))
      (testing "入力エコー（paper-fold-moon episode の claim 束縛先）"
        (is (= 0.1 (get-in outputs [:initial-thickness-mm :quantity])))
        (is (= "mm" (get-in outputs [:initial-thickness-mm :unit])))
        (is (= 42.0 (get-in outputs [:fold-count :quantity])))
        (is (= "count" (get-in outputs [:fold-count :unit])))))))

(deftest pi-monte-carlo-deterministic
  (testing "seed 固定で再現可能（同じ case は常に同じ出力）かつ π に収束"
    (let [case- {:solver {:kind :numeric-experiment}
                 :domain {:experiment :pi-monte-carlo :seed 42 :samples 20000}}
          a (rom/solve case- constants)
          b (rom/solve case- constants)
          pi-est (get-in a [:outputs :pi-estimate :quantity])]
      (is (= a b))
      (is (< 3.0 pi-est 3.3)))))

(deftest missing-constant-throws
  (testing "定数テーブルに無い ref は黙って補完せず例外"
    (is (thrown? #?(:clj Exception :cljs js/Error)
                 (rom/solve {:solver {:kind :two-body-orbit}
                             :domain {:semi-major-axis-km 384400.0
                                      :primary :phobos-mass
                                      :secondary :moon-mass}}
                            constants)))))

(deftest high-fidelity-kind-delegates
  (testing "nagare/kudaki 系 kind は rom で解かない（黙って近似しない）"
    (is (thrown? #?(:clj Exception :cljs js/Error)
                 (rom/solve {:solver {:kind :fvm-simple} :domain {}}
                            constants)))))

(deftest run-cases-skips-exec-kinds
  (let [results (rom/run-cases
                 [{:id :a :solver {:kind :tidal-scaling} :domain {:distance-ratio 0.5}
                   :outputs {:tidal-force-ratio {:unit "ratio"}}}
                  {:id :b :solver {:kind :fvm-simple} :domain {}
                   :outputs {:velocity-field {:unit "m/s"}}}]
                 constants)]
    (is (contains? results :a))
    (is (not (contains? results :b)))))
