(ns kagaku.factcheck-test
  (:require [clojure.test :refer [deftest is testing]]
            [kagaku.factcheck :as factcheck]
            [kagaku.pipeline :as pipeline]))

(def episode
  {:series :moon-approach
   :question "もし月が今の半分の距離まで近づいたら？"
   :duration-s 300
   :claims
   [{:id :tidal-ratio :text "潮汐力は8倍"
     :value {:quantity 8.0 :unit "ratio"}
     :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}}
    {:id :current-distance :text "平均距離は約38万4400km"
     :value {:quantity 384400.0 :unit "km"}
     :source {:kind :constant :ref :earth-moon-distance-mean}}]
   :sim-cases
   [{:id :moon-tidal
     :solver {:kind :tidal-scaling}
     :domain {:distance-ratio 0.5}
     :outputs {:tidal-force-ratio {:unit "ratio"}}}]
   :beats ["導入" "sim" "まとめ"]})

(def good-facts
  {:episode episode
   :sim-results {:moon-tidal {:outputs {:tidal-force-ratio
                                        {:quantity 8.0 :unit "ratio"}}}}
   :constants {:earth-moon-distance-mean
               {:quantity 384400.0 :unit "km" :source "NASA Moon Fact Sheet"}}
   :citations {}
   :render {:duration-s 298 :integrated-lufs -15.2 :true-peak-dbtp -1.4}
   :metadata {:made-for-kids false
              :sim-disclosure true
              :voice :voicevox
              :description "全編シミュレーションによる科学解説。VOICEVOX:四国めたん / VOICEVOX:ずんだもん"}})

(deftest gate-green-on-good-facts
  (let [{:keys [decision failed]} (factcheck/gate good-facts)]
    (is (= :ready-for-review decision))
    (is (empty? failed))))

(deftest gate-rejects-sim-mismatch
  (testing "台本の数値が sim 結果と食い違ったら reject"
    (let [facts (assoc-in good-facts
                          [:sim-results :moon-tidal :outputs
                           :tidal-force-ratio :quantity] 4.0)
          {:keys [decision failed]} (factcheck/gate facts)]
      (is (= :rejected decision))
      (is (some #(= :sim-consistency (:check %)) failed)))))

(deftest gate-rejects-unknown-constant
  (let [facts (assoc good-facts :constants {})
        {:keys [decision failed]} (factcheck/gate facts)]
    (is (= :rejected decision))
    (is (some #(= :constants (:check %)) failed))))

(deftest gate-rejects-missing-sim-disclosure
  (testing "シミュレーションであることの明示は誠実性の不変条件"
    (let [facts (assoc-in good-facts [:metadata :sim-disclosure] false)
          {:keys [decision failed]} (factcheck/gate facts)]
      (is (= :rejected decision))
      (is (some #(= :sim-disclosure (:check %)) failed)))))

(deftest gate-rejects-made-for-kids
  (let [facts (assoc-in good-facts [:metadata :made-for-kids] true)]
    (is (= :rejected (:decision (factcheck/gate facts))))))

(deftest gate-rejects-missing-voicevox-credit
  (let [facts (assoc-in good-facts [:metadata :description]
                        "全編シミュレーションによる科学解説。")]
    (is (= :rejected (:decision (factcheck/gate facts))))))

(deftest unverified-citation-rejected
  (let [facts (-> good-facts
                  (update-in [:episode :claims] conj
                             {:id :cheetah :text "チーターの最高速は約112km/h"
                              :value {:quantity 112.0 :unit "km/h"}
                              :source {:kind :citation :ref "Sharp 1997, J Zool"}})
                  (assoc :citations {}))]
    (is (= :rejected (:decision (factcheck/gate facts))))
    (testing "人間確認済みの引用なら通る"
      (let [facts' (assoc facts :citations
                          {"Sharp 1997, J Zool"
                           {:verified-by "Jun Kawasaki" :at "2026-07-16"}})]
        (is (= :ready-for-review (:decision (factcheck/gate facts'))))))))

(deftest publish-held-without-human-approval
  (testing "決定論 gate が全 green でも人間承認なしでは publish しない"
    (let [{:keys [decision reason]} (factcheck/publish-decision good-facts)]
      (is (= :held decision))
      (is (= :awaiting-human-review reason)))))

(deftest publish-with-human-approval
  (let [facts (assoc good-facts :human-approved
                     {:by "Jun Kawasaki" :at "2026-07-16T18:00+09:00"})
        {:keys [decision approved-by]} (factcheck/publish-decision facts)]
    (is (= :publish decision))
    (is (= "Jun Kawasaki" approved-by))))

(deftest pipeline-holds-at-human-review
  (testing "run-plan は human-review で :held 停止する（auto-publish しない）"
    (let [end (pipeline/run-plan {:episode episode :facts good-facts})]
      (is (= :held (:status end)))
      (is (= :awaiting-human-review (:reason end)))))
  (testing "承認済み facts なら :done まで到達"
    (let [facts (assoc good-facts :human-approved
                       {:by "Jun Kawasaki" :at "2026-07-16T18:00+09:00"})
          end (pipeline/run-plan {:episode episode :facts facts})]
      (is (= :done (:status end))))))

(deftest pipeline-rejects-invalid-sim-case
  (let [bad (assoc-in episode [:sim-cases 0 :solver :kind] :magic)
        end (pipeline/run-plan {:episode bad :facts good-facts})]
    (is (= :rejected (:status end)))))

(deftest provenance-gate-independent-of-render
  (testing "provenance-gate は render facts が無くても green になれる（尺・ラウドネスを見ない）"
    (let [no-render (dissoc good-facts :render)]
      (is (= :ready-for-review (:decision (factcheck/provenance-gate no-render))))))
  (testing "旧 gate（union）は render 欠如で reject（これが混在バグの正体だった）"
    (let [no-render (dissoc good-facts :render)]
      (is (= :rejected (:decision (factcheck/gate no-render)))))))

(deftest render-gate-checks-measurements
  (is (= :ready-for-review (:decision (factcheck/render-gate good-facts))))
  (testing "尺超過は render-gate が弾く"
    (is (= :rejected (:decision (factcheck/render-gate
                                 (assoc-in good-facts [:render :duration-s] 30)))))))

(deftest pipeline-halts-awaiting-exec-without-render
  (testing "render facts 無しの full-produce は :render-video で :awaiting-exec 停止（捏造しない）"
    (let [facts (dissoc good-facts :render)
          end (pipeline/run-plan {:episode episode :script nil :facts facts})]
      (is (= :awaiting-exec (:status end)))
      (is (= :render-video (:stage end)))
      (is (= :green (:gate end))))))

(deftest pipeline-full-with-render-holds-at-human-review
  (testing "render facts 付きなら render-gate を越えて human-review で hold"
    (let [end (pipeline/run-plan {:episode episode :facts good-facts})]
      (is (= :held (:status end)))
      (is (= :green (:render-gate end))))))
