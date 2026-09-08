(ns kagaku.voice-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.lang.text :as str]
            [kagaku.voice :as voice]
            [kagaku.script :as script]))

(deftest resolve-style-defaults
  (is (= 2 (voice/resolve-style-id "left" "normal")))
  (is (= 3 (voice/resolve-style-id "right" "normal")))
  (testing "emotion off（既定）は声色を切り替えない"
    (is (= 2 (voice/resolve-style-id "left" "happy")))
    (is (= 3 (voice/resolve-style-id "right" "surprised"))))
  (testing "emotion on で切替"
    (is (= 0 (voice/resolve-style-id "left" "happy" true)))
    (is (= 7 (voice/resolve-style-id "right" "surprised" true)))
    (testing "未知 emotion は base に落ちる"
      (is (= 2 (voice/resolve-style-id "left" "ecstatic" true))))))

(deftest line-plan-shape
  (let [p (voice/line-plan {:speaker "left" :text "こんにちは" :emotion "normal"} {})]
    (is (:ok p))
    (is (= 2 (:speaker p)))
    (is (= "四国めたん" (:speaker-name p)))
    (is (str/ends-with? (:audio-query-url p) "/audio_query"))
    (is (str/ends-with? (:synthesis-url p) "/synthesis"))
    (is (= {} (:query-overrides p)))))

(deftest line-plan-empty-text
  (is (= {:ok false :error :empty-text}
         (voice/line-plan {:speaker "left" :text "  " :emotion "normal"} {}))))

(deftest speed-pitch-overrides
  (let [p (voice/line-plan {:speaker "right" :text "ゆっくり" :emotion "normal"}
                           {:speed 0.9 :pitch 0.05})]
    (is (= {:speedScale 0.9 :pitchScale 0.05} (:query-overrides p)))))

(def claims
  [{:id :tidal-ratio :text "8倍" :value {:quantity 8.0 :unit "ratio"}
    :source {:kind :sim :case :moon-tidal :output :tidal-force-ratio}}])

(def raw-script
  {:scenes
   [{:location "夜空" :action "登場"
     :lines
     [{:speaker "left" :text "潮汐力は {{tidal-ratio}} だよ" :emotion "normal"}
      {:speaker "right" :text "ええっ" :emotion "surprised"}]}]})

(deftest plan-over-expanded-script
  (testing "展開済み台本の各 line に plan が付き、数値は転記済みで placeholder が残らない"
    (let [expanded (script/expand raw-script claims)
          plans (voice/plan-script expanded)]
      (is (= 2 (count plans)))
      (is (every? :ok plans))
      (is (str/includes? (:text (first plans)) "8倍"))
      (is (not-any? #(str/includes? (:text %) "{{") plans)))))

(deftest credits-cover-both-speakers
  (let [plans (voice/plan-script (script/expand raw-script claims))]
    (is (= ["VOICEVOX:四国めたん" "VOICEVOX:ずんだもん"] (voice/credit-lines plans)))
    (is (str/includes? (voice/credit-string plans) "VOICEVOX:四国めたん"))
    (is (str/includes? (voice/credit-string plans) "VOICEVOX:ずんだもん"))))
