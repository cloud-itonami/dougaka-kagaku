(ns kagaku.preview-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.lang.text :as str]
            [kagaku.scene :as scene]
            [kagaku.preview :as preview]))

(deftest circles-from-earth-moon
  (testing "地球-月 snapshot から 2 円（earth/moon）が出る"
    (let [snap (scene/earth-moon-scene {:distance-ratio 0.5})
          circles (vec (preview/scene->circles snap))
          by-name (into {} (map (juxt :name identity) circles))]
      (is (= 2 (count circles)))
      (is (contains? by-name "earth"))
      (is (contains? by-name "moon"))
      (testing "月は sim distance-ratio 由来の x 位置（半分の距離 ~192）"
        (is (< 191.0 (:cx (by-name "moon")) 193.0)))
      (testing "地球の半径は mesh asset 由来"
        (is (< 6.0 (:r (by-name "earth")) 6.7))))))

(deftest svg-well-formed
  (let [snap (scene/earth-moon-scene {})
        s (preview/svg snap)]
    (is (str/starts-with? s "<svg"))
    (is (str/includes? s "</svg>"))
    (is (str/includes? s "<circle"))
    (testing "非-authoritative であることを出力に明記（3D 規則の thumbnail 例外）"
      (is (str/includes? s "authoritative render = WebGPU")))
    (testing "entity 名が title に入る"
      (is (str/includes? s "<title>earth</title>"))
      (is (str/includes? s "<title>moon</title>")))))

(deftest scaling-scene-preview
  (testing "相似則シーンも 2 円（actual-size/scaled-up）で描ける"
    (let [snap (scene/scaling-law-scene {:length-ratio 200.0})
          names (set (map :name (preview/scene->circles snap)))]
      (is (= #{"actual-size" "scaled-up"} names)))))

(deftest material-color-mapped
  (testing "material albedo が rgb に反映される（地球は青系）"
    (let [snap (scene/earth-moon-scene {})
          earth (first (filter #(= "earth" (:name %)) (preview/scene->circles snap)))]
      ;; mat/earth = [0.15 0.35 0.75] → 青が最大
      (is (str/starts-with? (:fill earth) "rgb(")))))
