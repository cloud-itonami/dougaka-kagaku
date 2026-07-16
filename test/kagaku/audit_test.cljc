(ns kagaku.audit-test
  (:require [clojure.test :refer [deftest is testing]]
            [kagaku.audit :as audit]))

(def episode
  {:series :moon-approach
   :question "半分の距離？"
   :duration-s 300
   :claims
   [{:id :tidal :text "8倍" :value {:quantity 8.0 :unit "ratio"}
     :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}}]
   :sim-cases
   [{:id :moon-tidal :solver {:kind :tidal-scaling}
     :domain {:distance-ratio 0.5}
     :outputs {:tidal-force-ratio {:unit "ratio"}}}]
   :beats ["a" "b"]})

(def script
  {:scenes [{:lines [{:speaker "left" :text "潮汐は {{tidal}} だ" :emotion "normal"}
                     {:speaker "right" :text "へえ" :emotion "normal"}]}]})

(deftest clean-episode-passes
  (let [r (audit/audit-episode {:episode episode :script script})]
    (is (:pass? r))
    (is (= 1.0 (:score r)))
    (is (empty? (:findings r)))))

(deftest sim-unit-mismatch-caught
  (testing "claim unit と sim-case 宣言 unit の drift を spec 時点で捕える（iter9 の学びの前倒し）"
    (let [bad (assoc-in episode [:claims 0 :value :unit] "N")
          r (audit/audit-episode {:episode bad :script script})]
      (is (not (:pass? r)))
      (is (some #(re-find #"sim-unit-declared" %) (:findings r))))))

(deftest unspoken-claim-caught
  (testing "台本で言及されない数値 claim を coverage axis が捕える"
    (let [ep (update episode :claims conj
                     {:id :orphan :text "9.66日"
                      :value {:quantity 9.66 :unit "day"}
                      :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}})
          r (audit/audit-episode {:episode ep :script script})]
      (is (not (:pass? r)))
      (is (some #(re-find #"claim-coverage" %) (:findings r))))))

(deftest scene-linked-for-moon
  (let [r (audit/audit-episode {:episode episode :script script})
        scene-axis (first (filter #(= :scene-linked (:id %)) (:axes r)))]
    (is (= 1.0 (:score scene-axis)))))

(deftest preview-renderable-axis
  (testing "moon-approach scene は preview で SVG 化できる（circles 非空）"
    (let [r (audit/audit-episode {:episode episode :script script})
          ax (first (filter #(= :preview-renderable (:id %)) (:axes r)))]
      (is (= 1.0 (:score ax)))))
  (testing "scene 未対応 series は skip 満点（preview も見ない）"
    (let [ep {:series :future-tech :question "x" :duration-s 300
              :claims [{:id :a :text "y" :value {:quantity 1.0 :unit "ratio"}
                        :source {:kind :constant :ref :standard-gravity}}]
              :sim-cases []}
          r (audit/audit-episode {:episode ep})
          ax (first (filter #(= :preview-renderable (:id %)) (:axes r)))]
      (is (= 1.0 (:score ax))))))

(deftest citation-pending-does-not-fail-pass
  (testing "引用 PENDING は pass? を落とさない（人間確認待ちは正常状態）が findings には出す"
    (let [ep (update episode :claims conj
                     {:id :cite :text "x" :value {:quantity 1.0 :unit "ratio"}
                      :source {:kind :citation :ref "要確認"}})
          ep (update ep :scenes identity)
          sc (update script :scenes conj {:lines [{:speaker "left"
                                                   :text "{{cite}}" :emotion "normal"}]})
          r (audit/audit-episode {:episode ep :script sc :citations {}})]
      (is (:pass? r))                       ; blocker ではない
      (is (< (:score r) 1.0))               ; スコアには効く
      (is (some #(re-find #"PENDING" %) (:findings r))))))

(deftest audit-all-aggregates
  (let [r (audit/audit-all [{:episode episode :script script}
                            {:episode episode :script script}])]
    (is (:all-pass? r))
    (is (= 1.0 (:mean-score r)))
    (is (= 2 (count (:episodes r))))))

(deftest no-script-skips-coverage
  (testing "台本なしでも spec だけで audit でき、coverage/script は skip 満点"
    (let [r (audit/audit-episode {:episode episode})]
      (is (:pass? r)))))
