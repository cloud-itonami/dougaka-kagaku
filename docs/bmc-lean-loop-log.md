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
