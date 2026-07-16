# ai-gftd-dougaka-kagaku — BMC / Lean Loop 反復ログ（standalone パターン）

superproject の共有 BMC システム（`70-tools/bmc/`、gftdcojp 11 プロダクト）には
意図的に登録しない（base datoms への追加は人間レビュー事項 — CLAUDE.md BMC 節、
先例: local-murakumo ADR-2607121600 / net-babiniku ADR-2607122300 / dougaka-kodomo）。
このファイルが repo-local の append-only 反復ログ。既存 iteration は編集・削除しない。
**捏造ゼロ**: 計測していない値は unknown と書く。outcome/metric を LLM が推定で
埋めることを禁止する。

## Iteration 1 — 2026-07-16

**Build（今回作った・検証したもの）**:

- 設計 ADR-2607165000 accepted + scaffold push（pure-planner core、
  scenario/simcase/factcheck/pipeline、nbb 26→35 tests green）。
- `kagaku.rom`（reduced-order solver: :tidal-scaling / :two-body-orbit /
  :scaling-law / :numeric-experiment）を実装 — Phase A の sim 経路が実際に動く。
- E2E runner（`tools/run_episode.cljs`）で `content/tsuki-half-distance.edn` を
  実走: scenario validate OK / claim-consistency 全 OK（周期 claim 9.66 日 vs
  sim 9.6466 日、相対誤差 0.14% ≤ 2% gate）/ 定数出典 OK / bench datom 台帳
  初回 append（`docs/sim-benchmark-ledger.edn`）。
- solver 妥当性の物理チェック: 現在の月（a=384400km）で公転周期 27.28 日を
  再現（実測の恒星月 27.32 日、二体円軌道近似で 0.15% 差）— テストで固定。

**Measure（現状の実測値）**:

- 動画公開数: 0。チャンネル: 未開設。収益: $0。
- sim 経路のカバレッジ: rom 4 kinds 動作 / :exec 委譲 kinds（nagare/kudaki/fea）
  は未配線。
- bench 台帳: 2 datoms（tidal-scaling / two-body-orbit、いずれも wall-ms 0 =
  ms 分解能未満。重い solver が乗るまで性能シグナルなし）。

**Learn（仮説と次の検証）**:

- H1: 「LLM 非算術 + sim provenance」は追加コストほぼゼロで機能する
  （E2E 1 本で転記誤差 0.14% を機械検出できた — 支持。ただし n=1）。
- H2（未検証）: VOICEVOX + kami-engine 可視化で「見られる」科学解説になるか —
  次反復以降で voice/visual 経路を配線して 1 本目の限定公開まで進める。

**Next**: generate-script の L/R スキーマ流用（yukkuri から）、
render-sim-visual の kami-engine render-IR 変換の最小実装、D1 schema 検討。

## Iteration 2 — 2026-07-16

**Build**:

- `kagaku.script` — L/R 掛け合い台本 planner（yukkuri generate_script の
  {:scenes [{:lines [{:speaker :text :emotion}]}]} スキーマ継承）。新規約:
  **生テキストへのアラビア数字（半角・全角）直書き禁止**、数値言及は
  `{{claim-id}}` placeholder のみ、展開（`script/expand`）が factcheck 済み
  claim 値から機械転記。全数値 claim の言及必須（死に claim 検出）。
  LLM request spec（`script/request-spec`）も pure data で実装。
- pipeline `:generate-script` stage に script 検証を配線（episode + script の
  両方が valid でないと前進しない）。
- E2E runner に台本検証 + 展開表示を追加。`content/tsuki-half-distance-script.edn`
  （9 行の実台本）で実走: validate OK、展開後テキストに 8倍 / 9.66日 /
  384400km が転記され placeholder 残留なし。
- tests 35 → 43（78 assertions）green（nbb）。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
台本経路のカバレッジ: planner + 検証 + 展開まで動作。LLM 実呼び出し
（murakumo text）と TTS は未配線。

**Learn**:

- H1 補強: 数字直書き禁止 + placeholder 転記は、台本という「LLM が最も
  数値を捏造しやすい箇所」を構造的に塞げた（n=1、実 LLM 出力での違反率は
  未計測 — 実配線後に validate reject 率を測る）。
- 既知の穴: 漢数字（「三十個」等）は digit 検査を通過する。今回の実台本では
  正しい近似（384400/12742≈30.2）だったが、機械検証はされていない —
  human-review の重点確認項目として次反復以降で telop/notes に明示する案。

**Next**: (b) 2 本目 episode（:animal-power flea-jump、:scaling-law）E2E、
または (c) render-sim-visual の最小 render-IR 変換。

## Iteration 3 — 2026-07-16

**Build**:

- 2 本目 episode `content/nomi-jump.edn` +台本（:animal-power / :scaling-law、
  「ノミが人間サイズならビルを飛び越える」の訂正）。E2E 実走: scenario OK /
  claim-consistency 4 件 OK / script validate OK / bench datom append
  （台帳 2 → 3 datoms）。
- `docs/citations.edn`（引用台帳）新設 + runner の citation PENDING 表示。
  この episode の生物データ（ノミの体長・跳躍高）は物理定数でないので
  `:citation` 出所にし、**人間の出典確認が済むまで公開経路に乗らない**状態で
  正しく停止することを実地で確認（設計どおりの hold）。agent は
  `:verified-by` を書き込まない旨をファイル冒頭に明記。
- `:scaling-law` に `:length-ratio`（入力エコー）と
  `:strength-to-weight-penalty`（語り向け逆数表現）出力を追加。
- tests 43 → 44（82 assertions）green。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 2 本・台本 2 本・rom 4 kinds・bench 3 datoms（全て wall-ms 0 =
ms 分解能未満、性能シグナルは依然 unknown）。実 LLM 出力での script validate
reject 率は未計測（LLM 未配線）。

**Learn（今回の実台本で実際に発火した 2 つの実害）**:

- **iteration 2 で「既知の穴」と書いた漢数字の算術が、次の台本で即座に発火した。**
  「およそ八百五十倍」「体長の百倍も跳ぶ」— どちらも digit 検査を素通りする
  未検証の算術だった。対処: (a) 体長比は solver の `:length-ratio` エコー出力に
  束縛して claim 化（domain を変えて台本が古いままなら claim-consistency が
  検出することをテストで固定）、(b) 派生比（跳躍高÷体長）は語りから削除。
  → 学び: 「既知の穴」は記録するだけでは塞がらない。次に触るコンテンツで必ず出る。
- **単位の和訳と語りの不整合**: `{{power-penalty}} 分の一` が「850倍 分の一」と
  展開され意味が反転していた（unit "ratio"→"倍"）。転記の機械化は数値の正しさは
  守るが、**語としての正しさは守らない** — placeholder 前後の助詞・語順は
  human-review の確認対象として残る。
- 未対処の設計ギャップ: **claim から claim を導く「派生 claim」の出所種別が無い**
  （跳躍高 20cm ÷ 体長 2mm = 100 のような算術）。今回は語りから削除して回避したが、
  `:derived {:from [ids] :op ...}` を決定論評価する案が次の候補。

**Next**: (c) render-sim-visual の最小 kami-engine render-IR 変換、
または `:derived` 出所種別の実装（上記ギャップ）。

## Iteration 4 — 2026-07-16

**Build**:

- `:derived` 出所種別を実装（iteration 3 で特定した未対処ギャップ）。
  claim から claim を導く算術を LLM でなく決定論再計算する経路:
  - `kagaku.units` — 単位の次元テーブル + 換算（mm/cm/m/km, s/min/h/day/year,
    g/kg, m/s・km/h, N, ratio）。既知単位のみ、未知は nil で reject。
  - `kagaku.derived` — `:op` = :ratio | :sum | :diff | :product | :scale の
    決定論評価 + 宣言値との ±2% 照合。
  - scenario/validate に derived の malformed / from-unknown / self-reference
    検証を追加、factcheck に check-derived を配線、runner に再計算表示。
- nomi-jump に派生 claim `:jump-body-lengths`（跳躍高 20cm ÷ 体長 2mm = 100倍）を
  復活。iteration 3 で「語りから削除して回避」した科学解説の花形の数字が、
  今度は sim/citation の下流の派生値として**機械検証つきで**台本に戻った。
- tests 44 → 55（104 assertions）green。E2E: 派生再計算 OK、台本に「体長の
  100倍も跳ぶ」が展開。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
出所種別 4 種（sim/constant/citation/derived）全て機械検証つき。bench 台帳
5 datoms（nomi-jump は 2 ラン分 — append-only の別サンプル。wall-ms は
今回初めて 0 でない値 3〜7ms を記録したが、依然 scaling-law の軽演算で
性能シグナルとしては弱い。重い solver の実行は未配線で unknown のまま）。

**Learn**:

- iteration 3 の「派生 claim の出所が無い」ギャップは、実コンテンツ（nomi-jump）が
  実際に詰まっていた実害だった。塞いだ結果、跳躍高÷体長のような**単位をまたぐ
  派生値**が LLM 非算術のまま出せるようになった（20cm と 2mm を揃えて割るのは
  units の次元換算が担当。この換算は :derived 評価に限定し、sim 照合の
  「単位完全一致」方針は据え置き — 二つの厳しさを混ぜない）。
- 実装中に自分のコードで 1 バグ: `cond->` が `cons` の引数順を逆にして
  self-reference エラーメッセージが seqable でなくなった。テストが即座に捕捉
  （55 tests のうち scenario-test が赤 → concat + when に書き直して緑）。
  「AI は堂々と間違える」がツール実装側でも起きる — テストが安全網。

**Next**: (B) render-sim-visual の最小 kami-engine render-IR 変換、
または (C) 台本→VOICEVOX synthesize-voice の per-line request plan。

## Iteration 5 — 2026-07-16

**Build**:

- `kagaku.voice` — synthesize-voice stage の per-line VOICEVOX request plan。
  yukkuri voicevox.cljc の純ロジック（style_id カタログ / emotion→style /
  synthesize-plan / クレジット生成）を移植し kagaku 向けに調整:
  入力は**展開済み台本**（placeholder 転記後）、話者は left=四国めたん(2)/
  right=ずんだもん(3)、語り口は通常速度（ゆっくり実況ではない）。
- **VOICEVOX 商用クレジットを plan から機械生成**（`credit-string`）。
  factcheck の metadata チェック（description に 'VOICEVOX:' 必須）を満たす
  文字列が台本の話者構成から自動で出ることを実地確認 — クレジット取りこぼしを
  構造で防ぐ。
- pipeline `:synthesize-voice` stage に plan 生成を配線（:voice-plans /
  :voice-credits を state に積む）。runner に voice plan + クレジット表示。
- tests 55 → 61（126 assertions）green。E2E（tsuki）: 9 行すべてに plan、
  クレジット "VOICEVOX:四国めたん / VOICEVOX:ずんだもん" 生成、それが
  check-metadata を true にすることを確認。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
パイプライン被覆: compose→sim→script→factcheck→**voice(plan)** まで純データで
到達。実 IO（VOICEVOX /audio_query→/synthesis の 2 段 POST、または
murakumo /v1/audio/speech）は未配線 — 実音声の品質は unknown。

**Learn**:

- 今回は既存資産（yukkuri voicevox.cljc）の移植で、自分のバグ発火なし
  （前 2 反復は各 1 バグ）。移植元が pure logic として既に分離されていたのが
  効いた — yukkuri 側の「IO なし純ロジックを別 ns に隔離」設計の再利用価値。
- クレジットを「plan から生成 → factcheck が description で要求」の 2 点で
  閉じたことで、話者を足しても（例: 補助ナレーションに別話者）クレジットが
  自動追随する。metadata チェックが機械強制なので、人手のクレジット記入漏れ
  という事故クラスが消えた。
- 未配線の残り: 実 VOICEVOX 呼び出し / render-sim-visual（kami-engine
  render-IR）/ compose の topic 選定 / D1 永続化。次動画の E2E に最も近いのは
  render-sim-visual（sim 結果を絵にする経路）。

**Next**: (B) render-sim-visual の最小 kami-engine render-IR 変換
（kami-engine* / kami-scene-contracts の EDN 形式を先に調査）、
または (D) compose stage の topic 選定（yukkuri topics.cljc priorityScore 移植）。

## Iteration 6 — 2026-07-16

**Build**:

- `kagaku.scene` — render-sim-visual stage の scene planner。sim 結果を
  **KAMI scene snapshot（ECS-as-datoms）に変換**。地球-月シーン（地球球体 +
  月球体 + カメラ + 太陽光）を distance-ratio でパラメタライズし、月の x 位置を
  sim（tidal-scaling の distance-ratio）と連動させた。
- **既存 contract 調査を先に実施**（オーナー指示どおり独自形式を作らない）:
  `orgs/kotoba-lang/kami-contracts` の `kami.scene`（ARCHITECTURE.md §5）が
  authority と判明。attribute 語彙（:transform/translation, :mesh/asset,
  :camera/fov, :light/kind, :scene/env …）と snapshot 形
  {:snapshot/entities [..] :snapshot/assets [..]}、valid? の 4 規則（未知attr/
  dangling parent/dangling asset/複数 active camera/cycle）を消費者として mirror。
  kami-contracts を deps に引かず zero-dep を維持（repo-wide 3D 規則の
  「kami-engine stack を消費する側」に留まる）。
- pipeline `:render-sim-visual` stage 配線（scene 生成 → validate、担当外
  series は素通り）。tests 61 → 69（140 assertions）green。
- E2E: tsuki episode から scene 生成、entities [earth moon camera sun]、
  月 x=192.2 Mm（半分の距離）、kami.scene 語彙で valid。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
パイプライン被覆: compose→sim→script→factcheck→**scene(snapshot)**→voice(plan)
まで純データ。実 render（kami-engine headless / WebGPU）は未配線 — レンダ品質は
unknown。scene planner は現状 moon-approach のみ担当（他 series は nil を正直に
返す、未実装）。

**Learn**:

- (B) は「contract 調査が空振りなら独自形式の誘惑」を懸念していたが、
  **空振りではなかった** — kami-scene-contracts（ドメインカタログ主体で node/
  transform が無い）を最初に見て一瞬 swamp かと思ったが、kami-contracts の
  kami.scene に本命の ECS-as-datoms scene 契約（schema + snapshot + valid?）が
  あった。「最初に当たった repo が目的物とは限らない」— grep を複数 repo に
  広げたのが効いた。
- 消費者として語彙を mirror する設計（deps に引かず known-attrs と valid? 規則を
  authority 明記でコピー）は、zero-dep 維持と contract 準拠を両立できた。
  authority が進んだら追随する義務は docstring に明記。
- sim → visual の連動を「月の x = 平均距離 × distance-ratio」で結んだことで、
  台本の「8倍/9.66日」と画面の月位置が同じ sim パラメータ由来になった
  （数字と絵の出所が一致 = 科学解説の誠実性の一部を構造化）。

**Next**: (実 render 手前の純データ経路はほぼ揃った) 実 IO 配線の前段として
(D) compose の topic 選定（yukkuri topics.cljc priorityScore 移植）、または
scene planner の他 series 対応（animal-power の相似則 scene 等）。

## Iteration 7 — 2026-07-16

**Build**:

- **full-produce E2E 統合**（推奨 F）。run_episode.cljs に pipeline/run-plan を
  通す経路を追加し、episode から compose→sim→script→factcheck→voice→scene→
  render-video まで純データで畳んで各 stage 成果物を 1 コマンド表示。
- E2E 結果（両 episode で正しく動作）:
  - tsuki（constants + sim、未検証引用なし）→ render-video まで流れ
    `:awaiting-exec`（render 測定は :exec 実測待ち）。provenance green /
    scene moon-approach valid / voice 9 lines / credit 生成。
  - nomi-jump（引用未検証）→ :factcheck で `:rejected`（citations PENDING）。
    人間の出典確認まで公開経路に乗らない設計が pipeline 全体でも発火。
- tests 69 → 73（149 assertions）green。

**Learn（E2E 統合で実際に発火した設計欠陥 1 件）**:

- **factcheck が pre-render と post-render のチェックを混在させていた実バグを
  発見・修正。** `:factcheck` stage は stage 順で render-video の *前*（悪い
  数値に render 計算を使う前に止めるため）だが、`checks` に check-duration /
  check-loudness（render 後の測定＝`:render` facts 依存）が混ざっていた。
  実運用順では factcheck 時点で `:render` が無いので、**provenance が全 green
  でも尺・ラウドネス欠如で必ず :rejected** になる欠陥だった。単体テストは
  `:render` 込みの full facts を gate に渡していたので緑のまま隠れていた
  （＝テストが実運用の stage 順序を模していなかった死角）。
  修正: `provenance-gate`（render 前）/ `render-gate`（render 後）に分割、
  pipeline は :factcheck で provenance-gate、:render-video で render-gate。
  `gate`（union）は後方互換で温存。
- **render facts が無いとき :render-video は捏造せず `:awaiting-exec` 停止**に
  した。offline harness で loudness を 0 埋めして「通す」のは 捏造ゼロ 原則違反 —
  「実測待ち」を正直な終端状態にした。
- 学び: 「純データ経路が各 stage 単体で緑」でも「端から端で正しく繋がる」とは
  限らない。E2E 統合は単体テストの死角（stage 順序・facts の時間的可用性）を
  炙り出す。iteration 6 までの 6 部品が初めて 1 本に繋がって初めて出たバグ。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
パイプライン: compose→…→render-video まで純データで端から端まで到達確認。
実 IO（sim 実行以外: VOICEVOX / kami-engine render / YouTube / D1）は未配線。
render 後の測定・人間レビュー・publish は実 render 待ちで unknown。

**Next**: 実 IO 配線の最初の一歩（VOICEVOX /v1/audio/speech の実呼び出し 1 行、
または kami-engine headless render の 1 シーン）、または (D) compose topic 選定。

## Iteration 8 — 2026-07-16

**Build**:

- `kagaku.compose` — compose stage の topic 選定（純関数）。yukkuri
  topics.cljc の pick-topic（priorityScore 順 + 既出除外）を移植し、
  series.edn 形（{:series {:topics [{:id :q :priority}]}}）に合わせ、さらに
  **series ローテーション**（直近 series を penalty で軽く後回し、圧倒的高
  priority は覆さない）を追加。決定論（乱数・時刻なし、tie は series名→id 順）。
- `tools/next_topic.cljs` — 「次に何を作るか」を機械選定するツール。
  resources/series.edn + docs/produced.edn（作成済み topic-id の append-only
  リスト）から次 topic を選び episode 種を表示。
- `docs/produced.edn` 新設（tsuki=moon-half-distance, nomi=flea-jump 登録）。
- tests 73 → 82（166 assertions）green。実データ実行: 既出 2 件を除外し
  ant-strength（animal-power, p=85）を選定、episode 種生成。

**Learn**:

- これで pipeline の主要 stage（compose/design-sim/generate-script/factcheck/
  render-sim-visual/synthesize-voice）がすべて実装 or 明示的 passthrough に
  なった。compose は「pipeline 内 stage」でなく「pipeline の上流の選定関数」に
  置くのが正しい境界だった（episode を選んでから produce を回す）— pipeline の
  :compose は passthrough のままにし、選定は kagaku.compose に分離。
- series ローテーションの penalty 設計: priority 粒度（5〜90）に対し penalty 15 は
  「僅差の別 series を優先するが 90 vs 85 の圧は覆さない」バランス。テストで
  両方向（覆す/覆さない）を固定。無人運転で同 series が連続しない土台。
- 実害の発火なし（純関数の移植 + 決定論設計）。yukkuri 側が既に pick-topic を
  pure に分離していた資産が効いた（iteration 5 の voice 移植と同じ再利用効果）。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
pipeline 主要 stage は全実装。残る未配線は実 IO のみ（VOICEVOX 実呼び出し /
kami-engine 実 render / YouTube upload / D1）。「次に作る topic」は機械化された
（無人運転の入口）が、topic→claims/sim-cases/台本の自動生成（LLM 実呼び出し）は
未配線で unknown。

**Next**: 実 IO 配線の最初の一歩（VOICEVOX /v1/audio/speech を実際に叩いて
tsuki の 1 行を wav 化、または kami-engine headless で earth-moon を 1 枚 render）、
または (E) scene の animal-power 対応 / (G) 3本目 episode。

## Iteration 9 — 2026-07-16

**Build**:

- 3本目 episode `content/pi-monte-carlo.edn` +台本（:three-min-math、
  「円周率を乱数で求める」）。**全 claim が sim 由来の初の episode** —
  sample数・推定値・真πとの誤差、すべて :numeric-experiment solver の
  決定論出力（seed 42 再現可能）に束縛。台本の数字は 1 つも LLM が作っていない
  ＝LLM 非算術の理想形。
- rom :pi-monte-carlo に出力追加: :sample-count（エコー）、:abs-error
  （真 π = Math/PI との絶対誤差、数学定数なので solver が決定論計算）。
- 単位体系を拡張: "1"（純粋無次元数、バレ数表示）/ "count"（個数）を
  units テーブルと script の unit-ja に追加。
- E2E: claim-consistency 3件 OK、full-produce が render-video まで流れ
  :awaiting-exec（citation 無しなので tsuki と同じく完走）。tests 82→82
  （166→168 assertions、pi episode で 2 assertion 増）green。
- docs/produced.edn に :pi-monte-carlo 追記。

**Learn（実台本で発火した実害 2 件）**:

- **digit 検査が「3.14」を捕捉**（iteration 2 で価値を記録した安全網が3度目の
  発火）。台本の相槌「本当だ、3.14に近い！」の生数字を検出 → 「あの円周率に
  そっくりだ！」に修正。LLM でなく自分が書いた台本でも生数字は出る — 検査が
  効いている証拠。
- **単位 "ratio"→"倍" の誤適用**を E2E 表示で発見。円周率が「3.15176倍」、
  個数が「100000倍」と展開されていた（倍率でない純粋数に「倍」が付く）。
  unit "ratio" を「倍率」に限定し、純粋無次元数 "1"・個数 "count" を分離
  （バレ数表示）→「およそ 3.15176」「100000 個」に修正。
  学び: 転記の機械化は数値の正しさを守るが、**単位の意味づけの正しさ**は
  別問題。数値の種類（倍率/純粋数/個数/物理量）に応じた表示語彙が要る。
  これは iteration 2 の「語としての正しさは守らない」学びの単位版。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 3本（うち pi は全 claim sim 由来）。bench 台帳に numeric-experiment の
wall-ms 39（10万サンプル、初めて意味のある計算時間 — ただし rom の軽演算なので
sim スタック性能の本格シグナルにはまだ遠い）。実 IO 未配線は変わらず。

**Next**: (E) scene の animal-power 対応、または実 IO 配線の一歩
（VOICEVOX 実呼び出し / kami-engine 実 render）、または (H) kagaku.audit
（episode 横断の self-audit）。

## Iteration 10 — 2026-07-16

**Build**:

- `kagaku.audit` — episode 横断の決定論 self-audit（network-isekai
  isekai.ux.audit / design-quality.audit の「計測されないメトリクス＝劇場」
  思想を移植）。7 axes を weighted 集約: spec-valid / script-valid /
  **sim-unit-declared**（新）/ derived-sound / scene-linked / citations-verified
  / claim-coverage。各 axis は 0..1 score + 具体的 findings。
- **sim-unit-declared axis が iteration 9 の学びを spec レベルに前倒し**:
  :sim claim の unit が sim-case の宣言 output unit と一致するかを検査。
  iter9 では unit drift を実 run の表示まで気付かなかったが、この axis は
  spec 時点（run 前）で捕える。
- `tools/audit.cljs` — content/*.edn 全 episode を 1 コマンド検査する CI runner。
  1 つでも pass? false なら exit 1。tests 82→89（183 assertions）green。
- 全 3 episode で実行: tsuki 100% / pi 100% / nomi 90%（citation PENDING を
  正しく可視化、pass? は落とさない＝人間確認待ちは正常状態）。ALL PASS、
  mean-score 97%。

**Learn**:

- pass?（公開ブロッカー判定）と score（品質メトリクス）を分離した設計が効いた:
  citation PENDING は score を下げるが pass? は落とさない。「人間の出典確認待ち」は
  不正ではなく正常な中間状態、という設計意図（factcheck の human-review hold と
  同じ）を audit でも一貫させた。
- audit は既存 validate 群（scenario/script/derived/scene）の再実行 + 新 axis
  （sim-unit-declared / claim-coverage / scene-linked）の合成。既存の純関数を
  「横断 fitness function」として束ねられたのは、各 stage を no-IO pure に
  保ってきた設計の配当。
- 今回も過去反復の学びが axis 化された（iter2 の raw-digit → script-valid、
  iter9 の unit drift → sim-unit-declared、iter2 の unspoken claim →
  claim-coverage）。「学びを記録 → 次で発火 → 機械チェック化」の 3 段が
  この loop で繰り返し起きている。self-audit はその機械チェックの集約点。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 3本、self-audit mean-score 97%（citation 2件 PENDING が唯一の減点、
これは人間の出典確認という設計どおりの TODO で不正ではない）。CI で episode
追加時の回帰が機械保証されるようになった。実 IO 未配線は変わらず。

**Next**: 実 IO 配線の一歩（VOICEVOX localhost:50021 synthesize の実験、
無ければ未確認と報告）、または (E) scene の animal-power 対応、
または CI ワークフロー（.github/workflows で audit.cljs + test を回す）。

## Iteration 11 — 2026-07-17

**Build**:

- `.github/workflows/ci.yml` — push / PR で nbb テスト（test/run.cljs）+
  episode 横断 self-audit（tools/audit.cljs）を回す CI。既存の
  kotoba-lang/design-quality の ci.yml を参照して形式を合わせた（setup-node +
  nbb 第一経路）。
- README に self-audit / full-produce E2E / next_topic の実行例と CI の説明を追記。
- ローカル検証（CI と同一コマンド）: nbb テスト 89 green、self-audit ALL PASS
  mean 97%、いずれも exit 0。

**Learn（実際に発火した実害 1 件）**:

- **`npx --yes nbb --classpath src:test test/run.cljs` が失敗**。npx が
  スラッシュ入りスクリプトパス `test/run.cljs` を **パッケージ名
  `github:test/run.cljs` と誤解**して install を試み、コケた（design-quality の
  ci.yml は `-m ns.name` 形式だったのでこの罠を踏んでいなかった）。
  対処: CI で `npm install -g nbb` してから `nbb` を直接呼ぶ形に変更。
  ローカルの nbb（install 済み）は最初から直接呼んでいたので緑で、npx 経路
  だけが踏む罠だった — 「ローカルで通る」と「CI の呼び出し形式で通る」は別、を
  実地で確認（iter7 の「単体で緑 ≠ 端から端で緑」の CI 版）。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
CI ワークフロー追加済み。**GitHub Actions 上での実際の緑は push 後に確認が必要
（このローカル環境では Actions を実行できない）— 現時点では unknown、ローカルで
CI と同一の nbb コマンドが緑であることのみ確認済み**。無人運転の品質ゲートは
「ローカル同一コマンド緑 + workflow 追加」まで到達、Actions 実緑は follow-up。

**Next**: Actions の実緑を push 後に確認、または (E) scene の animal-power 対応
（audit の scene-linked が nomi でも満点に）、または (I) VOICEVOX 実 IO 実験。

### Iteration 11 追記（push 後の Actions 確認）

- push 後に `gh run list` を確認 → **run が 0 件**。原因を API で特定:
  `gh api repos/gftdcojp/ai-gftd-dougaka-kagaku/actions/permissions` が
  **`{"enabled":false}`** — この private repo は **GitHub Actions が無効化**
  されている。したがって ci.yml は登録されても発火しない。
- 正直な現状: **CI ゲートは「ローカルで CI と同一の nbb コマンドが緑」まで。
  GitHub Actions 上の実行は repo 設定で無効なので実緑は得られていない（unknown
  でなく、明確に「動かない」）。** 無人運転の自動品質ゲートにするには (a) repo の
  Actions を有効化する（オーナー操作）か (b) ローカル/cron 側で
  `nbb tools/audit.cljs` を回す運用にする、のどちらかが要る。follow-up。
- workflow file 自体は正しい（ローカルで同一コマンド緑を確認済み）ので、
  Actions を有効化すれば動く見込み。yml は残置し、有効化を follow-up とする。

## Iteration 12 — 2026-07-17

**Build**:

- `kagaku.scene` の animal-power 対応。相似則 scene（`scaling-law-scene`）:
  実物大 vs 拡大版の 2 球体を並置し、拡大版の半径を length-ratio の
  log 圧縮（`scaled-radius` = base × (1 + log10 r)）でスケール（850 倍を実寸で
  置くと画面外になるため。概念可視化で数値の正は sim claim 側）。
- `scene-for-episode` を series ディスパッチ化（case: :moon-approach /
  :animal-power / 他は nil）。**nomi-jump に実シーンが付いた** — これまで
  audit の scene-linked axis は nomi で「未対応 skip 満点」だったが、今は
  実シーン（actual-size / scaled-up / camera / sun、valid）を検証する満点に。
- tests 89→92（191 assertions）green。audit は全 3 episode ALL PASS を維持
  （nomi は依然 90%、減点は citation PENDING のみ＝人間確認待ちで正常）。

**Learn**:

- テストで `:asset/inline`（pr-str した EDN 文字列）を read-string して半径を
  取り出す設計にしかけたが、cljs で `read-string` が cljs.core に無く portable で
  ないため、半径計算を純関数 `scaled-radius` として露出させてそれを直接テスト。
  「観測しやすさのために内部を pure 関数として切り出す」のは今回も有効
  （iter6 の length-ratio エコー、iter8 の compose 分離と同じ設計手）。
- log 圧縮の選択は正直に「概念可視化」と docstring/BMC に明記した。850 倍を
  線形に置くと画面外になるので log で潰したが、これは絵の都合であって
  数値の正ではない — 数値は sim claim（722500倍/614125000倍）が持つ。
  絵と数字の役割分担（絵＝直感、数字＝sim由来の正）を崩さない。
- Actions 無効の制約下でローカル完結タスクを選んだのは正解だった
  （nomi の scene-linked が実シーンで埋まり、audit の被覆が上がった）。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
scene 対応 series: moon-approach + animal-power の 2/5（three-min-math /
everyday-mechanism / future-tech は未対応で skip、正直に nil）。audit
mean-score 97% 維持。実 IO 未配線は変わらず。

**Next**: (I) VOICEVOX 実 IO 実験（localhost:50021、無ければ未確認と報告）、
または scene の他 series 対応、または 4本目 episode（ant-strength）。

## Iteration 13 — 2026-07-17

**Build**:

- `tools/try_voice.cljs` — VOICEVOX 実 IO 実験ツール（実 IO 配線の最初の一歩）。
  kagaku.voice/line-plan が返す audio-query-url/synthesis-url を実際に叩き、
  1 行を wav 化して bytes 長・RIFF header を報告。エンジン未起動なら「未確認」で
  exit 0（環境依存で実害でない設計）。
- README に try_voice の実行例を追記。tests 92 green（ツール追加のみ、lib 不変）。

**Measure（実 IO を初めて実測 — 全反復で最大の空白だった箇所）**:

- **この環境の VOICEVOX engine は稼働していた**（localhost:50021、version 0.25.2、
  ADR-2607131645 の常駐と一致）。
- **kagaku.voice の plan contract が実エンジンで通った**: 四国めたん
  （style_id=2）で「円周率はおよそ3.15。点を増やすほど近づきます。」を合成 →
  **214572 bytes の本物の WAVE audio**（`file` 判定: RIFF little-endian, WAVE,
  Microsoft PCM, 16bit mono 24000Hz）。audio_query → synthesis の 2 段 POST が
  plan の記述どおり成立。
- 出力 wav は scratchpad に書き repo を汚さない。

**Learn**:

- iteration 5 で kagaku.voice を「pure planner、実 IO は :exec」と設計したが、
  その plan（audio-query-url/synthesis-url/speaker/query-overrides）が
  **実エンジンで実際に通ることを初めて実測**できた。純データ設計は「実 IO を
  後回しにする言い訳」ではなく「plan の contract が正しければ実 IO は薄い配線で
  済む」ことの実証になった（今回の配線は fetch 2 回だけ）。
- yukkuri voicevox.cljc から移植した style_id カタログ・URL 組み立てが、
  移植先でも実エンジンに対して正しかった（yukkuri の運用実績が効いた）。
- これで実 IO の 1 モダリティ（音声）が「plan → 実バイト」まで通った。残る
  未実測の実 IO: kami-engine 実 render（画）/ YouTube upload / D1 永続化。
  ただし本ツールは実験用で、pipeline の :synthesize-voice stage を実 IO 化した
  わけではない（stage は依然 plan のみ返す純データ）— 実運用の :exec 配線は
  別スコープ。

**Measure（続き）**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
実 IO: 音声のみ plan→実wav を実測（他モダリティは unknown）。

**Next**: (L) scene の three-min-math 対応、(K) 4本目 episode ant-strength、
または kami-engine 実 render の実験（scene snapshot → 実画像、engine の
headless render 経路を調査）。

## Iteration 14 — 2026-07-17

**Build**:

- 4本目 episode `content/ant-strength.edn` +台本（:animal-power、
  「アリはなぜ体重の何十倍も運べるのか」）。next_topic の機械選定
  （iter8）が推奨した topic を実際に作った＝選定と制作が繋がった。
- **全 claim が既存 rom :scaling-law 出力で固まる**（length-ratio 200 =
  アリを人サイズに拡大）: size-up 200倍 / area-up 4万倍 / mass-up 800万倍 /
  power-penalty 200倍。nomi-jump（拡大フレーミング）と同型で全数値が「倍」
  表示で正しく出る。
- **通説「体重の50倍」は :citation にも sim にも置かず定性表現「何十倍」に留めた**
  （断面積 L^2 と体積 L^3 の増え方の差から「小さいほど相対的に強い」を sim で
  示す構成。誠実さの設計判断）。nomi が :citation PENDING を持つのと対照的に、
  ant は生物データを数値 claim にしないので citation ゼロ＝audit 100%。
- animal-power scene（iter12）が付き、full-produce E2E が render-video まで
  流れて :awaiting-exec（scene animal-power valid / voice 9行 / provenance green）。
- produced.edn に :ant-strength 追記。tests 92 green（episode 追加のみ、lib 不変）。
  audit 全4episode ALL PASS、**mean-score 97%→98%**（ant 100% が押し上げ）。

**Learn**:

- **全 stage 揃いの episode が 2 本になり（tsuki / ant）、full-produce の
  再現性が確認できた**。scene/voice/audit を持つ series（moon-approach /
  animal-power）では episode 追加が「spec + 台本を書くだけ」で全 stage 緑まで
  到達する — 部品が揃った series での episode 量産コストが低いことを実証。
- next_topic の機械選定（priorityScore + rotation）が実際の制作対象と一致した。
  「次に何を作るか」の機械化（iter8）→「実際に作る」（iter14）が一周した。
- 誠実さの設計反復: nomi は生物データを :citation にして PENDING で正しく
  ブロックしたが、ant は「相似則の帰結」に振ることで生物データの数値化を回避し
  citation ゼロにできた。**同じジャンルでも構成次第で人間検証依存を減らせる**
  （sim で示せる部分を増やす）— これはチャンネルの無人運転しやすさに直結。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 4本（tsuki/nomi/pi/ant、うち tsuki/ant が全 stage 揃い）。audit
mean 98%。実 IO は音声のみ実測（iter13）、他モダリティ未実測。

**Next**: (M) kami-engine 実 render 実験（scene→実画像）、(L) three-min-math
scene、または 5本目 episode（next_topic 推奨: everyday-mechanism kettle-whistle
だが nagare :fvm-simple は rom 非対応で :exec 委譲 — claims の固め方に工夫要）。

## Iteration 15 — 2026-07-17

**調査結果（kami-engine 実 render 経路）— 正直に**:

- **kami-engine-render は archived プレースホルダ**（ADR-2607102200 addendum 8-9、
  west group `archived`、「Do not add new code here」）。実体は `kotoba-lang/webgpu`
  （ブラウザ WebGPU executor）/ `kami-engine-sdk`（ECS/render-IR）に移動済み。
- **実 render 経路は headless Chromium + Playwright + WebGPU（macOS は Metal
  backend）+ コンパイル済み `.kotoba` guest**（`wasm-webcomponent/test/render/
  verify-render-*.mjs`、`webgpu-harness.mjs`）。index.html をロードし canvas を
  screenshot → PNG decode してピクセル検証する重い経路。`npm install` +
  `npx playwright install chromium` が前提。
- **結論: kagaku の scene snapshot を実 WebGPU で render してピクセルバイトを出す
  のは本反復の範囲外**（重い harness を立てる必要があり、プロンプトが許可した
  「調査止まり」ケース）。実 render のピクセルは **未実測（unknown）**。

**Build（実 render の代わりに出せた軽い成果）**:

- `kagaku.preview` — scene snapshot → **非-authoritative な 2D SVG サムネイル**
  （human-review 用）。正射影正面ビューで mesh を円に、material albedo を色に、
  env clear を背景色に落とす。repo-wide 3D 規則が**明示的に許可する
  『thumbnail / diagram / 非3D preview』例外**に限定し、docstring・SVG コメント・
  ツール出力の 3 箇所で「authoritative render は WebGPU/kami-engine」と明記。
- `tools/try_render.cljs` — episode の scene を SVG に書き出す。tsuki で実行:
  地球（青大円 cx=0 r=6.371）+ 月（灰小円 cx=192.2=半分の距離 r=1.737）、
  背景 env clear、502 bytes の妥当な SVG。sim distance-ratio がサムネイル上の
  月位置に正しく反映（絵が sim 連動）。
- tests 92→96（204 assertions）green。audit ALL PASS mean 98% 維持。

**Learn**:

- 実 IO の 2 モダリティ目（画）は音声（iter13、plan→実wav 成功）と違い、
  **authoritative 経路が重すぎて本反復では実測できなかった**。正直に「調査止まり・
  実 render 未実測」と記録し、捏造した「render できた」を書かない。
- ただし「実 render は無理でも human-review を助ける安価な preview は出せる」を
  3D 規則の thumbnail 例外の範囲で実現。**authoritative（WebGPU）と
  non-authoritative（SVG thumbnail）を役割・ラベルで厳密に分けた** — 絵の正は
  WebGPU、配置の一目確認は SVG、と混同させない（iter14 の「絵=直感・数字=sim」
  の役割分担の render 版）。
- SVG サムネイルが sim distance-ratio 連動を保った（月 cx=192.2）ので、
  human-review 段で「数字（8倍/9.66日）と絵（月の位置）が同じ sim 由来か」を
  WebGPU 無しでも目視確認できる。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
実 IO: 音声=plan→実wav 実測済み、画=**authoritative WebGPU render は未実測**
（非-authoritative SVG thumbnail のみ）。YouTube/D1 も未実測。

**Next**: 実 WebGPU render の harness を立てる（重い、別途スコープ）、
(L) three-min-math scene、または 5本目 episode。

## Iteration 16 — 2026-07-17

**Build**:

- `kagaku.scene` の three-min-math 対応。π モンテカルロ点群シーン
  （`monte-carlo-scene`）: 単位正方形に点を打ち、四分円の内（x²+y²≤1）外で
  色分け（inside 青 / outside 灰）。**点は rom :pi-monte-carlo と同一の決定論
  LCG（同 seed → 同じ点配置）で生成**するので、絵と sim が同じ乱数列を共有
  （数字と絵の出所一致、moon/animal と同じ思想）。ortho カメラ使用。
- `scene-for-episode` に :three-min-math ディスパッチ（pi-monte-carlo
  experiment のみ、他 experiment は nil を正直に返す）。
- pi-monte-carlo episode に実 scene が付き、**audit の scene-linked が
  90%→100%**（pi は全 stage 揃いの 3 本目に）。try_render で 200 点の
  SVG（25047 bytes、内外色分け）も出力。
- tests 96→99（215 assertions）green。audit 全4episode ALL PASS mean 98%。

**Learn（実際に発火した実害 1 件）**:

- **audit の scene-linked が `:camera/ortho-h` を unknown-attr として弾いた**。
  kagaku.scene の `known-attrs`（kami.scene 語彙の mirror）に `:camera/ortho-w`
  / `:camera/ortho-h`（kami.scene schema に実在）を写し忘れていた。monte-carlo
  シーンで初めて ortho カメラを使ったので発火。**self-audit（iter10）が
  自分の mirror 漏れを機械検出した** — audit を作った配当が別 iteration の
  バグ検出で返ってきた。mirror に ortho-w/h を追加して緑。
- 「mirror は authority のサブセット」設計（iter6）の弱点＝写し漏れが、
  実使用（ortho 初投入）で顕在化。新しい attr を使うたび mirror 追加が要る。
  authority を deps に引かない zero-dep の代償だが、audit が検出網になる構図。
- 点群を「小球体多数」で表現したのは kami.scene が点群 primitive を持たない
  ため（汎用 mesh の範囲で概念表現）。絵は概念（200点）、数値は sim（10万
  サンプル）と点数を分けた — 絵と数字の役割分担を点数でも守った。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
scene 対応 series: moon-approach + animal-power + three-min-math = **3/5**。
全 stage 揃い episode: tsuki / ant / pi = 3本。audit mean 98%。実 IO は
音声のみ実測（画の authoritative WebGPU は未実測、iter15）。

**Next**: 残る scene 未対応 series（everyday-mechanism / future-tech）、
full-produce E2E への SVG 出力統合、または 5本目 episode。

## Iteration 17 — 2026-07-17

**Build**:

- 5本目 episode `content/paper-fold-moon.edn` +台本（:three-min-math、
  「紙を42回折ると月に届く、は本当か」）。**sim + constant + derived の
  3 出所種別を 1 本で全部使う初の episode**: paper-thickness/fold-count/
  final-thickness（sim :fold-to-moon）+ moon-distance（constant）+ exceeds-by
  （derived :ratio [final-thickness moon-distance] = 1.14）。指数増加の直感に
  反する強さを sim で示す。
- rom :fold-to-moon に入力エコー出力追加（:initial-thickness-mm / :fold-count）。
- E2E: 全 claim 検証 OK（sim 3件 / constant 1件 / derived 1件）、derived が
  「44万km ÷ 38万km = 1.14倍」を再計算一致。full-produce は render-video まで
  流れて :awaiting-exec（three-min-math scene は pi 専用なので本 episode は
  scene skip、audit 100%）。
- produced.edn に :paper-fold-moon 追記。tests 99→99（215→219 assertions、
  rom echo テスト 4 追加）green。audit 全5episode ALL PASS mean 98%。

**Learn（実台本で発火した実害 1 件）**:

- **digit 検査が 5 行の生数字を捕捉**（「42回」「2倍」「44万キロ」等）。
  安全網の 4 度目の発火。対処の型が固まってきた: (a) 数値 claim は placeholder
  化（42回→{{fold-count}}）、(b) 定義的な機構説明の数（折ると「2倍」）は
  漢数字「二倍」に（これは検証すべき data 値でなく操作の定義なので可）、
  (c) 数値の相槌の繰り返し（「44万キロ！？」）は定性表現に（「そんなに
  大きくなるの！？」）。iteration 9 の「漢数字による未検証算術は禁止」との
  区別: 二倍は fold 操作の定義（増加率そのもの）で算術結果ではないので許容。
- **5 出所（sim/constant/citation/derived）のうち citation 以外の 3 つを
  1 episode で同時に使えた**。derived が sim claim と constant claim を跨いで
  比を取れた（final-thickness は sim、moon-distance は constant、同 km 次元で
  ratio）— 出所種別が混在しても derived の単位次元チェックが機能する実証。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 5本（tsuki/nomi/pi/ant/paper）。全 stage 揃い（scene 付き）: tsuki/ant/pi
の3本（paper は three-min-math だが pi 専用 scene なので scene なし）。audit
mean 98%。実 IO は音声のみ実測。

**Next**: (O) full-produce E2E への SVG 出力統合、(Q) audit preview-renderable
axis、または everyday-mechanism/future-tech の scene 対応 or 6本目 episode。

## Iteration 18 — 2026-07-17

**Build**:

- audit に **preview-renderable axis** 追加。scene を持つ episode で
  `kagaku.preview/scene->circles` と `svg` が例外なく通り、circles が空でなく
  SVG が `<svg` で始まるかを検査。scene が「valid だが描けない」退行
  （iter16 の ortho 属性漏れのような、snapshot は valid でも preview 側で
  落ちる/空になるクラス）を先回りで防ぐ。scene 未対応 series は skip 満点。
- audit を kagaku.preview の消費者にした（iter15 で作った preview を audit が
  初めて機械的に使う）。weight 再配分（scene-linked 0.10→0.08、
  preview-renderable 0.07 新設、citations 0.10→0.05）。
- tests 99→100（221 assertions）green。audit 全5episode ALL PASS、
  **mean-score 98%→99%**（citation weight 減で nomi の減点が小さくなった）。

**Learn**:

- iter16 で手動発見した「scene は valid だが preview 側で ortho 属性が
  未知で落ちる」退行クラスを、**axis として機械化**した。これで scene と
  preview の両方が緑でないと episode が pass? しない（preview-renderable は
  citations と違い blocker）。「学びを記録→次で発火→機械チェック化」の 3 段
  （iter2/9/16 で観測）を、今回は自分で作った 2 つのモジュール（scene/preview）
  の**結合点**の退行に適用 — 単一モジュール内でなくモジュール間の contract 退行も
  audit で守れる。
- pass? の設計（citations だけ非-blocker、他は blocker）が効いている:
  preview-renderable を blocker にしたのは「scene があるのに絵にできない」のは
  公開前に直すべき欠陥だから。citation PENDING（人間確認待ち＝正常）とは
  区別が明確。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
audit axes 8個（spec/script/sim-unit/derived/scene-linked/preview-renderable/
citations/coverage）。mean 99%。scene 対応 3/5 series、全 stage 揃い episode
tsuki/ant/pi。実 IO は音声のみ実測。

**Next**: everyday-mechanism/future-tech の scene 対応、full-produce E2E への
SVG 出力統合、または 6本目 episode。

## Iteration 19 — 2026-07-17

**Build**:

- full-produce E2E（run_episode.cljs）への **human-review 用 SVG 出力統合**。
  pipeline/run-plan の終端 state に :scene snapshot があれば、
  kagaku.preview の SVG を scratchpad に自動書き出し（`kagaku-review-<episode>.svg`）
  してパスを表示。**human-review の材料＝sim 数値 + 台本展開 + scene 画 + credit が
  1 コマンドで揃う**。scene 未対応 episode は skip（paper で確認）。
- 本チャンネルの要（人間が事実確認する設計、ADR の human-review 無条件 hold）の
  実運用を回しやすくする締めくくり機能。SVG は非-authoritative（authoritative
  render は WebGPU）と明記済み（iter15 の preview をそのまま消費）。
- README 追記。tests 100 green（runner 追加のみ、lib 不変）。E2E 実測:
  tsuki は scene 画 SVG 書き出し + provenance/scene/voice 緑で :awaiting-exec、
  paper は scene none で SVG skip。

**Learn**:

- full-produce E2E が「純データ経路の疎通確認」（iter7）から「human-review の
  材料出し」に育った。1 コマンドで (a) sim 数値の claim 整合、(b) 台本の
  placeholder 展開後テキスト、(c) VOICEVOX 音声 plan と credit、(d) scene の
  配置 SVG が全部出る。人間はこれを見て事実確認 → 承認（:human-approved）を
  facts に入れれば publish 判定に進む、という運用フローの入力が揃った。
- iter15 で「実 render は重くて未実測、代わりに SVG thumbnail」とした判断が、
  ここで human-review 材料として実際に活きた。「authoritative は無理でも
  review を助ける非-authoritative preview」の価値が具体化。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
full-produce E2E が human-review 材料（数値/台本/音声plan/scene画）を 1 コマンドで
出せる。実 IO は音声のみ実測、画は非-authoritative SVG のみ（authoritative
WebGPU render は未実測）。

**Next**: future-tech/everyday-mechanism の scene or episode 対応、
実運用の human-review→approve→publish フローの :exec 側配線（別スコープ）、
または 6本目 episode。

## Iteration 20 — 2026-07-17

**Build**:

- rom に **`:reduced-order-aero` solver 実装**（抗力方程式 F=½ρCdAv²）。
  空気密度 ρ は constants の air-density-sea-level（**出典 ISA sea level 15°C、
  1.225 kg/m³**）× density-ratio。density-ratio で真空チューブの減圧を表現し、
  抗力が ρ に線形なことを使う。Cd/前面積/速度は工学前提（domain 入力、
  代表値を note に明記）。出力: drag-force-n / drag-power-kw / speed 各エコー。
  rom-kinds に追加、simcase は既に有効 kind として宣言済み。
- units に力・仕事率単位（N/kN/W/kW）追加。
- 6本目 episode `content/hyperloop-drag.edn`（:future-tech、「真空チューブ列車
  はなぜ速いのか」）。**future-tech series 初の sim episode**。open-air と
  vacuum-tube の 2 sim case を計算し、derived :ratio で抗力 1000倍削減を示す
  （sim 4 claim + derived 1）。時速1000km で開放抗力 66kN・18MW、チューブ内
  66N。「気圧を1/1000にすると抵抗も1/1000」を全 sim 由来で。
- tests 100→101（226 assertions）green。audit 全6episode ALL PASS mean 99%。
  produced.edn 追記。**4/5 series が sim で解ける**（everyday-mechanism の
  nagare :fvm-simple のみ rom 非対応、正直に残す）。

**Learn**:

- チャンネルの副目的「kotoba-lang sim スタックの性能を試す」（ADR）に沿って
  rom を 1 solver 拡張。**抗力方程式は物理係数（空気密度）を constants から
  引き、係数の出典（ISA）を明記**した — 「AI に算術をさせない」の物理係数版
  （ρ を LLM が覚えた値でなく出典付き定数から取る）。Cd/前面積は工学前提と
  明示して物理定数と区別。
- derived が **2 つの別 sim case を跨いで比を取れた**（open-drag と tube-drag、
  同 N 次元 → ratio 1000）。iter17 は sim+constant 跨ぎ、今回は sim+sim（別
  case）跨ぎ — derived の適用範囲が広いことの再確認。
- digit 検査が相槌の「1万8千」「6万から66」を捕捉（安全網 5 度目）。数値の
  繰り返し相槌は定性表現に、が定着した対処型。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 6本（tsuki/nomi/pi/ant/paper/hyperloop）。sim 対応 series 4/5。
audit mean 99%。bench 台帳に reduced-order-aero の 2 datom。実 IO は音声のみ実測。

**Next**: everyday-mechanism（nagare :fvm-simple、rom 非対応 = :exec 委譲の
扱いを試す）、future-tech の scene 対応、または hyperloop に scene。

## Iteration 21 — 2026-07-17

**Build**:

- **:exec 委譲 episode パターンの初例**を確立。7本目 episode
  `content/kettle-whistle.edn`（:everyday-mechanism、「やかんはなぜ鳴る？」）。
  笛の流れは CFD（nagare :fvm-simple）でしか解けず rom 非対応。そこで:
  - sim-case は :fvm-simple で**宣言**する（:exec が後で流れを可視化する用）が、
    **数値 claim をそこに束縛しない**。
  - 数値は :citation（笛の実測周波数 ~3000Hz、人間検証必須）と :constant
    （音速 343m/s、出典 constants）のみ。
  - 役割分担: sim =「絵（流れの可視化）」、citation/constant =「数値」。
- E2E で設計どおり動作: scenario valid（:fvm-simple case 込み）/ sim claim 無で
  claim-consistency 空 / constant OK / citation PENDING で **factcheck が正しく
  :rejected**（人間の出典確認待ち）/ bench 空（rom 未実行 = 正直、CFD は :exec で
  bench）。audit は pass?（citation 非-blocker）95%。
- produced.edn 追記。tests 101 green（episode 追加のみ、lib 不変）。audit
  全7episode ALL PASS mean 99%。

**Learn**:

- **rom で解けない題材（CFD 系 everyday-mechanism）を隠さず扱う道が開いた**。
  「sim-case は宣言するが claim を束縛しない、数値は citation/constant」という
  分離で、全 5 series が（sim 直解 4 + :exec 委譲 1 で）カバー可能に。
  sim スタックが解けない領域を「解けるふり」せず、citation で人間検証に回す
  正直な構成 — チャンネルの誠実性の設計（AI 非算術）と一貫。
- この pattern は既存の部品（citation gate / sim-case validation / rom skip）の
  組合せだけで成立した（新コードゼロ）。iter1-20 で積んだ各 stage の pure 設計が、
  新しい episode 型を「データを書くだけ」で受け入れられる柔軟性を持っていた。
- kettle は full-produce で :rejected（citation PENDING）だが、これは nomi と
  同じ「正常な人間待ち」状態。audit は pass?（構造 OK）で citation を減点のみ、
  full-produce gate は :rejected（公開ブロック）— 2 つの gate の役割差が明確。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 7本。series カバレッジ 5/5（sim 直解 4: moon/animal/three-min/future、
:exec 委譲 1: everyday-mechanism）。audit mean 99%。実 IO は音声のみ実測。

**Next**: 全 series カバー達成。次は深さ方向（各 series の 2 本目、scene 拡充、
実 IO の 2 モダリティ目 kami render harness）または cadence 運用設計。

## Iteration 22 — 2026-07-17

**Build**:

- `kagaku.scene` の future-tech 対応。ハイパーループ・チューブシーン
  （`hyperloop-scene`）: x 軸に並ぶマーカー球列（チューブ）+ 列車球 + 目的地球。
  **目的地までの距離を sim speed-mps に連動**（速いほど遠くへ、/10 で scene 単位に
  圧縮）。数値の正は sim claim（抗力・1000×削減）が持ち、scene は「チューブを走る」
  直感を担当。
- `scene-for-episode` に :future-tech ディスパッチ（sim-case の speed-mps から）。
  **hyperloop-drag episode が全 stage 揃いに**（scene future-tech valid、
  full-produce E2E で human-review SVG 自動出力、audit scene-linked +
  preview-renderable 満点 → 100%）。SVG は 18 円（16 tube markers + 列車 + 目的地）。
- tests 101→104（236 assertions）green。audit 全7episode ALL PASS mean 99%。
  **scene 対応 4/5 series**（moon/animal/three-min/future、everyday-mechanism は
  :exec 委譲で scene 無し）。全 stage 揃い episode: tsuki/ant/pi/hyperloop の4本。

**Learn**:

- iter19 の human-review SVG 統合 + iter18 の preview-renderable axis が、
  future-tech scene を足しただけで hyperloop にそのまま効いた。**scene を 1 つ
  足すと（human-review SVG 出力・audit の 2 axis・full-produce の締め）が
  自動で付いてくる**構造ができている — 各機能が scene-for-episode という
  1 点で繋がっているので、series 追加のコストが逓減する。
- 「絵は概念・数字は sim」の役割分担を future-tech でも維持: 目的地距離は
  speed 連動（絵の直感）だが、削減比 1000× や抗力 66kN は sim claim（数値の正）。
  scene の距離スケール（/10 圧縮）は絵の都合で数値ではない、と設計に内包。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 7本、scene 4/5 series、全 stage 揃い 4本（tsuki/ant/pi/hyperloop）。
audit mean 99%。実 IO は音声のみ実測。

**Next**: cadence 運用（daily_draft スクリプト）、各 series 2本目で深さ、
または 実 IO の 2 モダリティ目（kami render harness、重い）。

## Iteration 23 — 2026-07-17

**Build**:

- rom に **`:roche-limit` solver 実装**（ロッシュ限界＝潮汐力が衛星の自己重力を
  超え引き裂く距離）。剛体 d=R(2ρp/ρs)^⅓ / 流体 d=2.44R(ρp/ρs)^⅓。**密度は
  constants の質量・半径から計算**（ρ=M/(4/3πR³）— LLM が覚えた密度でなく
  出典付き定数から導く、AI 非算術の徹底）。地球密度 5513 / 月密度 3342 kg/m³
  （いずれも実測一致）。
- 8本目 episode `content/moon-roche.edn`（moon-approach 2本目、「月はどこまで
  近づけるのか」）。全 claim = sim（限界距離 剛体9485/流体18368km・密度）+
  constant（現在距離）+ derived（余裕比 20.9倍）。**moon-approach scene（既存）が
  付き全 stage 揃い**、full-produce で human-review SVG 自動出力、audit 100%。
- tests 104→105（240 assertions）green。audit 全8episode ALL PASS mean 99%。

**Learn（実際に発火した実害 1 件）**:

- **rom-kinds に :roche-limit を足したが simcase の solver-kinds に足し忘れ**、
  full-produce が :design-sim で :rejected（simcase/validate-cases が未知 solver
  として弾いた）。claim-consistency 単体は通っていた（rom は解けた）ので気付き
  にくかったが、**pipeline の design-sim stage が simcase 語彙で検証する**ため
  そこで露見。solver 追加は 2 箇所（rom-kinds = 解ける集合、simcase/solver-kinds =
  episode が宣言してよい集合）の同期が要る、を実地で確認。前者だけ足すと
  「rom は解けるが episode 宣言が弾かれる」不整合になる。
- 物理の正しさ: ロッシュ限界を :tidal-scaling（力比のみ）でなく専用 solver で
  正しく計算。密度を定数から導いたので、値が実測（剛体 ~9500km / 流体 ~18400km）
  と一致 — 「AI が覚えた値」でなく「定数からの計算」で正しさを担保。derived が
  sim（限界距離）と constant（現在距離）を跨いで余裕比を出した（iter17/20 と同型）。

**Measure**: 動画公開数 0 / チャンネル未開設 / 収益 $0（変化なし）。
episode 8本、rom solver 6 kinds、全 stage 揃い 5本（tsuki/ant/pi/hyperloop/roche）。
moon-approach series は 2本に。audit mean 99%。実 IO は音声のみ実測。

**Next**: 各 series の深さ（2本目）、cadence 運用、または rom の :road-load /
:rom-fc 追加で future-tech の別 topic。
