(ns kagaku.script-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.lang.text :as str]
            [kagaku.script :as script]))

(def claims
  [{:id :tidal-ratio :text "距離が半分になると潮汐力は8倍になる"
    :value {:quantity 8.0 :unit "ratio"}
    :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}}
   {:id :current-distance :text "月までの平均距離は約38万4400km"
    :value {:quantity 384400.0 :unit "km"}
    :source {:kind :constant :ref :earth-moon-distance-mean}}])

(def good-script
  {:scenes
   [{:location "夜空" :action "登場"
     :lines
     [{:speaker "left" :text "距離は {{current-distance}} だよ" :emotion "normal"}
      {:speaker "right" :text "半分になると {{tidal-ratio}} なの！？" :emotion "surprised"}]}]})

(deftest valid-script-passes
  (is (:valid? (script/validate good-script claims))))

(deftest raw-digits-forbidden
  (testing "生テキストへの数字直書き（半角・全角とも）は弾く — LLM の暗算を台本に書かせない"
    (let [bad (assoc-in good-script [:scenes 0 :lines 0 :text] "距離は384400kmだよ")
          {:keys [valid? errors]} (script/validate bad claims)]
      (is (not valid?))
      (is (some #(= :line/raw-digits-forbidden (first %)) errors)))
    (is (not (:valid? (script/validate
                       (assoc-in good-script [:scenes 0 :lines 0 :text] "距離は３８万kmだよ")
                       claims))))))

(deftest unknown-claim-placeholder-fails
  (let [bad (assoc-in good-script [:scenes 0 :lines 0 :text] "距離は {{made-up-number}} だよ")
        {:keys [valid? errors]} (script/validate bad claims)]
    (is (not valid?))
    (is (some #(= :line/claim-unknown (first %)) errors))))

(deftest all-numeric-claims-must-be-spoken
  (testing "言及されない数値 claim は死に claim として弾く"
    (let [bad (update-in good-script [:scenes 0 :lines]
                         (fn [lines] [(first lines)]))
          {:keys [valid? errors]} (script/validate bad claims)]
      (is (not valid?))
      (is (some #(= [:claims/unspoken :tidal-ratio] %) errors)))))

(deftest invalid-speaker-and-emotion
  (is (not (:valid? (script/validate
                     (assoc-in good-script [:scenes 0 :lines 0 :speaker] "center")
                     claims))))
  (is (not (:valid? (script/validate
                     (assoc-in good-script [:scenes 0 :lines 0 :emotion] "ecstatic")
                     claims)))))

(deftest expand-transcribes-values
  (testing "placeholder は claim 値から機械転記される（8.0 ratio → 8倍）"
    (let [expanded (script/expand good-script claims)
          texts (map :text (get-in expanded [:scenes 0 :lines]))]
      (is (= "距離は 384400km だよ" (first texts)))
      (is (= "半分になると 8倍 なの！？" (second texts)))
      (is (not-any? #(str/includes? % "{{") texts)))))

(deftest fmt-value-cases
  (is (= "8倍" (script/fmt-value {:quantity 8.0 :unit "ratio"})))
  (is (= "9.66日" (script/fmt-value {:quantity 9.66 :unit "day"})))
  (is (= "384400km" (script/fmt-value {:quantity 384400.0 :unit "km"})))
  (testing "純粋無次元数(\"1\")と個数(\"count\")は接尾辞なし＝バレ数（円周率に「倍」を付けない）"
    (is (= "3.15176" (script/fmt-value {:quantity 3.15176 :unit "1"})))
    (is (= "100000" (script/fmt-value {:quantity 100000.0 :unit "count"})))))

(deftest request-spec-lists-claims
  (let [{:keys [system user]} (script/request-spec
                               {:question "もし月が半分の距離に来たら？"
                                :claims claims
                                :beats ["導入" "sim"]})]
    (is (str/includes? system "禁止"))
    (is (str/includes? user "{{tidal-ratio}}"))
    (is (str/includes? user "{{current-distance}}"))))
