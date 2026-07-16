(ns kagaku.scene-test
  (:require [clojure.test :refer [deftest is testing]]
            [kagaku.scene :as scene]))

(deftest earth-moon-valid
  (testing "生成した snapshot は kami.scene 語彙・規則に適合"
    (let [snap (scene/earth-moon-scene {:distance-ratio 1.0})
          {:keys [valid? errors]} (scene/validate snap)]
      (is valid? (str errors)))))

(deftest half-distance-moves-moon
  (testing "distance-ratio が sim と連動し、月の x 位置に反映される"
    (let [full (scene/earth-moon-scene {:distance-ratio 1.0})
          half (scene/earth-moon-scene {:distance-ratio 0.5})
          moon-x (fn [s] (->> (:snapshot/entities s)
                              (filter #(= "moon" (:kami/name %)))
                              first :transform/translation first))]
      (is (< 383.0 (moon-x full) 386.0))
      (is (< 191.0 (moon-x half) 193.0))
      (is (< (moon-x half) (moon-x full))))))

(deftest exactly-one-active-camera
  (let [snap (scene/earth-moon-scene {})
        actives (filter :camera/active? (:snapshot/entities snap))]
    (is (= 1 (count actives)))))

(deftest asset-refs-resolve
  (testing "mesh/material の ref がすべて assets に存在（dangling なし）"
    (is (:valid? (scene/validate (scene/earth-moon-scene {}))))))

(deftest unknown-attr-rejected
  (testing "authority 語彙外の attr を足すと弾く（独自拡張の混入防止）"
    (let [snap (scene/earth-moon-scene {})
          bad (update snap :snapshot/entities
                      (fn [es] (mapv #(assoc % :my/custom-thing 1) es)))
          {:keys [valid? errors]} (scene/validate bad)]
      (is (not valid?))
      (is (some #(= :unknown-attr (first %)) errors)))))

(deftest dangling-asset-rejected
  (let [snap (scene/earth-moon-scene {})
        bad (assoc snap :snapshot/assets [])  ; asset 定義を消す
        {:keys [valid? errors]} (scene/validate bad)]
    (is (not valid?))
    (is (some #(= :dangling-asset (first %)) errors))))

(deftest multiple-cameras-rejected
  (let [snap (scene/earth-moon-scene {})
        extra {:kami/eid (random-uuid) :kami/name "cam2" :camera/active? true
               :transform/translation [0.0 0.0 0.0]}
        bad (update snap :snapshot/entities conj extra)]
    (is (not (:valid? (scene/validate bad))))))

(deftest scene-for-episode-moon
  (testing "moon-approach episode の distance-ratio から scene を導く"
    (let [ep {:series :moon-approach
              :sim-cases [{:id :moon-tidal :solver {:kind :tidal-scaling}
                           :domain {:distance-ratio 0.5}}]}
          snap (scene/scene-for-episode ep)
          moon-x (->> (:snapshot/entities snap)
                      (filter #(= "moon" (:kami/name %))) first
                      :transform/translation first)]
      (is (< 191.0 moon-x 193.0))
      (is (:valid? (scene/validate snap)))))
  (testing "担当外 series は nil を正直に返す"
    (is (nil? (scene/scene-for-episode {:series :three-min-math :sim-cases []})))))

(deftest scaling-law-scene-valid
  (testing "相似則 2 球体シーンが kami.scene 語彙で valid"
    (let [snap (scene/scaling-law-scene {:length-ratio 850.0})]
      (is (:valid? (scene/validate snap)))
      (is (= #{"actual-size" "scaled-up" "camera" "sun"}
             (set (map :kami/name (:snapshot/entities snap))))))))

(deftest scaling-scene-bigger-with-ratio
  (testing "length-ratio が大きいほど拡大版の球体半径が大きい（sim 連動、log 圧縮）"
    (is (< (scene/scaled-radius 10.0) (scene/scaled-radius 1000.0)))
    (is (= scene/base-radius-mm (scene/scaled-radius 1.0)))))

(deftest scene-for-episode-animal-power
  (testing "animal-power episode の length-ratio から相似則 scene を導く"
    (let [ep {:series :animal-power
              :sim-cases [{:id :flea-scaling :solver {:kind :scaling-law}
                           :domain {:length-ratio 850.0}}]}
          snap (scene/scene-for-episode ep)]
      (is (some? snap))
      (is (:valid? (scene/validate snap)))
      (is (= 1 (count (filter :camera/active? (:snapshot/entities snap)))))))
  (testing "length-ratio を持たない animal-power は nil（正直）"
    (is (nil? (scene/scene-for-episode {:series :animal-power :sim-cases []})))))
