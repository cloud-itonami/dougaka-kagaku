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
