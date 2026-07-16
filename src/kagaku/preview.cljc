(ns kagaku.preview
  "scene snapshot → **非-authoritative** な 2D SVG サムネイル（human-review 用）。

   ⚠ これは authoritative な 3D レンダリングではない。repo-wide 3D 規則
   （ADR-2607102200）の authoritative render は WebGPU/WGSL（kami-engine stack、
   実経路は wasm-webcomponent の headless Chromium + WebGPU harness）であり、
   本 ns はその規則が明示的に許可する『thumbnail / diagram / 非3D preview』に限る。
   目的は human-review 段で scene の配置（どの球体がどこに、どの大きさで）を
   WebGPU を立てずに一目で確認できる安価なプレビューを出すこと。数値の正・
   最終画の正はあくまで sim claim と実 WebGPU render が持つ。

   投影は正射影の正面ビュー（world x→screen x, world y→screen y, z 破棄）。
   kagaku の scene は x 軸配置（地球-月 / 実物大-拡大）なので正面ビューで足りる。"
  (:require [clojure.string :as str]
            #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

(defn- inline-data [snapshot asset-id]
  (some-> (->> (:snapshot/assets snapshot)
               (filter #(= asset-id (:asset/id %))) first :asset/inline)
          edn/read-string))

(defn- ref-id [r] (if (vector? r) (second r) r))

(defn- rgb [albedo]
  (if (and (sequential? albedo) (= 3 (count albedo)))
    (let [[r g b] (map #(int (Math/round (* 255.0 (double %)))) albedo)]
      (str "rgb(" r "," g "," b ")"))
    "rgb(180,180,180)"))

(defn scene->circles
  "mesh を持つ entity を {:name :cx :cy :r :fill} の円に落とす（world 座標）。"
  [snapshot]
  (for [e (:snapshot/entities snapshot)
        :when (:mesh/asset e)
        :let [[x y _] (:transform/translation e)
              mesh (inline-data snapshot (ref-id (:mesh/asset e)))
              mat (inline-data snapshot (ref-id (:material/asset e)))]]
    {:name (:kami/name e)
     :cx x :cy y
     :r (or (:radius mesh) 1.0)
     :fill (rgb (:albedo mat))}))

(defn svg
  "snapshot → SVG 文字列（非-authoritative サムネイル）。opts {:w :h}。"
  ([snapshot] (svg snapshot {}))
  ([snapshot {:keys [w h] :or {w 640 h 360}}]
   (let [circles (vec (scene->circles snapshot))
         env (some-> (get-in snapshot [:snapshot/scene :scene/env]) edn/read-string)
         bg (rgb (:clear env))
         ;; world bbox（円の広がり）を viewbox にフィット（10% 余白）
         xs-lo (apply min (map #(- (:cx %) (:r %)) circles))
         xs-hi (apply max (map #(+ (:cx %) (:r %)) circles))
         ys-lo (apply min (map #(- (:cy %) (:r %)) circles))
         ys-hi (apply max (map #(+ (:cy %) (:r %)) circles))
         pad (* 0.1 (max (- xs-hi xs-lo) (- ys-hi ys-lo) 1.0))
         vb-x (- xs-lo pad) vb-y (- ys-lo pad)
         vb-w (+ (- xs-hi xs-lo) (* 2 pad))
         vb-h (+ (- ys-hi ys-lo) (* 2 pad))
         ;; SVG は y 下向きなので world y を反転（-cy）で正立させる
         circle-svg
         (str/join "\n"
                   (for [{:keys [name cx cy r fill]} circles]
                     (str "  <circle cx=\"" cx "\" cy=\"" (- cy) "\" r=\"" r
                          "\" fill=\"" fill "\"><title>" name "</title></circle>")))]
     (str "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" w "\" height=\"" h
          "\" viewBox=\"" vb-x " " (- (+ vb-y vb-h)) " " vb-w " " vb-h "\">\n"
          "  <rect x=\"" vb-x "\" y=\"" (- (+ vb-y vb-h)) "\" width=\"" vb-w
          "\" height=\"" vb-h "\" fill=\"" bg "\"/>\n"
          circle-svg "\n"
          "  <!-- 非-authoritative 2D projection preview (human-review). "
          "authoritative render = WebGPU / kami-engine. -->\n"
          "</svg>\n"))))
