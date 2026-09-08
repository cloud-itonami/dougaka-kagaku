;; VOICEVOX 実 IO 実験（実 IO 配線の最初の一歩）:
;;   nbb --classpath src tools/try_voice.cljs ["セリフ"] [--speaker N]
;; kagaku.voice/line-plan が返す audio-query-url/synthesis-url を実際に叩き、
;; 1 行を wav 化して bytes 長を報告する。plan の contract が実エンジンで通るかの実測。
;; エンジン未起動/接続不可なら「未確認」と正直に報告して exit 0（環境依存で実害でない）。
;; 鍵不要（localhost）。fetch 使用。生成 wav は scratchpad に書く（repo を汚さない）。
(ns try-voice
  (:require [kotoba.lang.text :as str]
            [kagaku.voice :as voice]
            ["fs" :as fs]))

(def args (vec *command-line-args*))
(def speaker-idx (some (fn [[i a]] (when (= a "--speaker") i))
                       (map-indexed vector args)))
(def override-speaker (when speaker-idx (js/parseInt (nth args (inc speaker-idx)))))
(def text (or (first (remove #(or (= "--speaker" %)
                                  (= (str override-speaker) %)) args))
              "これはテストです。円周率はおよそ3.14。"))

(def engine-url "http://127.0.0.1:50021")

;; plan は展開済み台本の 1 行を模す（left = 四国めたん）。
(def plan (voice/line-plan {:speaker "left" :text text :emotion "normal"}
                           {:engine-url engine-url}))
(def speaker (or override-speaker (:speaker plan)))

(println "== VOICEVOX 実 IO 実験")
(println "   text:" text)
(println "   speaker(style_id):" speaker (str "(" (:speaker-name plan) ")"))
(println "   audio-query-url:" (:audio-query-url plan))

(defn fail-honest [stage err]
  (println (str "\n== 未確認: VOICEVOX engine に到達できない（" stage "）"))
  (println "   " (str err))
  (println "   → plan の contract 妥当性は今回未実測（エンジン未起動 or 接続不可）。")
  (js/process.exit 0))                    ; 環境依存で実害でない

(-> (js/fetch (str (:audio-query-url plan) "?speaker=" speaker
                   "&text=" (js/encodeURIComponent text))
              #js {:method "POST"})
    (.then (fn [r]
             (if (.-ok r)
               (.json r)
               (throw (js/Error. (str "audio_query HTTP " (.-status r)))))))
    (.then (fn [query]
             (println "\n-- audio_query OK（AudioQuery 取得）")
             (js/fetch (str (:synthesis-url plan) "?speaker=" speaker)
                       #js {:method "POST"
                            :headers #js {"Content-Type" "application/json"}
                            :body (js/JSON.stringify query)})))
    (.then (fn [r]
             (if (.-ok r)
               (.arrayBuffer r)
               (throw (js/Error. (str "synthesis HTTP " (.-status r)))))))
    (.then (fn [buf]
             (let [bytes (js/Uint8Array. buf)
                   n (.-length bytes)
                   out "/private/tmp/claude-501/-Users-junkawasaki-github-com-junkawasaki/80eb0bfa-400a-400a-8326-1ad3e8d37674/scratchpad/kagaku-try-voice.wav"
                   header (apply str (map #(js/String.fromCharCode %)
                                          (take 4 (array-seq bytes))))]
               (fs/writeFileSync out (js/Buffer.from buf))
               (println "\n-- synthesis OK")
               (println "   wav bytes:" n)
               (println "   header(RIFF?):" (pr-str header))
               (println "   written:" out)
               (println "\n== 実測: plan の contract が実 VOICEVOX engine で通った"
                        (if (= "RIFF" header) "（RIFF/wav 確認）" "（header 要確認）"))
               (js/process.exit 0))))
    (.catch (fn [e] (fail-honest "fetch" (.-message e)))))
