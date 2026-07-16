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
