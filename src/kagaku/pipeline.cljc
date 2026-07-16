(ns kagaku.pipeline
  "produce パイプラインの stage-order と advance reducer。
   yukkuri graphs/produce.cljc / kodomo pipeline.cljc と同型の純データ骨格 —
   各 stage は実行計画（request spec / case EDN / ffmpeg plan / gate 判定）を
   返すだけで IO しない。実行は :exec 側（murakumo engines / cae.solver hosts /
   kami-engine headless render / renderer-mac / YouTube client）が担う。"
  (:require [kagaku.scenario :as scenario]
            [kagaku.simcase :as simcase]
            [kagaku.factcheck :as factcheck]))

(def stage-order
  ":factcheck は決定論 HARD gate（fail = :rejected、advisory 続行しない）。
   :human-review は無条件 HARD hold — オーナー承認が facts に入るまで :held。"
  [:compose            ; series topic 選定 → project 起票（priorityScore は yukkuri topics 踏襲）
   :research-brief     ; LLM 資料調査 brief + 引用候補（この段では数値主張を書かない）
   :design-sim         ; sim case EDN 設計（kagaku.simcase/validate-cases）
   :run-sim            ; :exec — cae.solver dispatch（nagare/kudaki/aero/echem/num）。bench datom 記録
   :generate-script    ; L/R 掛け合い台本（yukkuri generate_script スキーマ踏襲）。数値は必ず provenance ref
   :factcheck          ; 決定論 HARD gate（kagaku.factcheck/gate）
   :synthesize-voice   ; VOICEVOX 話し声（murakumo /v1/audio/speech）
   :render-sim-visual  ; sim 結果 → kami-engine render-IR → headless frames（WebGPU/WGSL first）
   :generate-visual    ; 補助図版・テロップ素材（ComfyUI）
   :arrange-bgm        ; BGM（ongakuka compose 選定）
   :compose-scene      ; timeline / cut / telop 合成 plan（ffmpeg filter graph）
   :render-video       ; renderer-mac ffmpeg fleet（dougaka contract）
   :human-review       ; 人間の事実確認 + 公開承認（無条件 hold）
   :publish            ; YouTube madeForKids=false・通常広告
   :audit])            ; 生成イベント + bench 台帳 append

(def parallel-asset-stages
  #{:synthesize-voice :render-sim-visual :generate-visual :arrange-bgm})

(defn next-stage [current]
  (->> stage-order (drop-while #(not= % current)) second))

(defn advance
  "1 stage 分 state を進める reducer。
   state = {:stage kw :episode spec :facts map :status :running|:held|:rejected|:done ...}"
  [{:keys [stage] :as state}]
  (case stage
    :design-sim
    (let [{:keys [valid? errors]}
          (simcase/validate-cases (get-in state [:episode :sim-cases]))]
      (if valid?
        (assoc state :stage (next-stage stage))
        (assoc state :status :rejected :errors errors)))

    :generate-script
    (let [{:keys [valid? errors]} (scenario/validate (:episode state))]
      (if valid?
        (assoc state :stage (next-stage stage))
        (assoc state :status :rejected :errors errors)))

    :factcheck
    (let [{:keys [decision failed]} (factcheck/gate (:facts state))]
      (if (= :ready-for-review decision)
        (assoc state :stage (next-stage stage) :gate :green)
        (assoc state :status :rejected :gate :flag :failed failed)))

    :human-review
    (let [{:keys [decision] :as verdict}
          (factcheck/publish-decision (:facts state))]
      (case decision
        :publish (assoc state :stage (next-stage stage) :approved verdict)
        :held (assoc state :status :held :reason :awaiting-human-review)
        (assoc state :status :rejected :failed (:failed verdict))))

    :audit
    (assoc state :status :done)

    ;; その他の stage は実行計画を :exec 側が消費した前提で前進のみ
    (if-let [nxt (next-stage stage)]
      (assoc state :stage nxt)
      (assoc state :status :done))))

(defn run-plan
  "全 stage を（gate/hold 停止まで）畳み込む。テスト/ドライラン用。"
  [initial-state]
  (loop [state (assoc initial-state :stage (first stage-order) :status :running)]
    (if (not= :running (:status state))
      state
      (let [state' (advance state)]
        (cond
          (not= :running (:status state')) state'
          (= (:stage state') (:stage state)) (assoc state' :status :stuck)
          :else (recur state'))))))
