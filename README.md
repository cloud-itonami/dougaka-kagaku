# ai-gftd-dougaka-kagaku — AI 科学解説チャンネル（sim-first）生成パイプライン

Mark Rober 型の「科学を面白く説明する」動画を 1 topic / 1 episode spec から
自動生成する。大型実物実験は AI 生成に向かないため、**実験はすべて
kotoba-lang sim スタック（CFD / 構造 / 軌道 / 数値実験）のシミュレーション**で
行い、その結果だけを台本の数値主張の出所にする。`ai-gftd-yukkuri` の
pure-planner アーキテクチャ（no-IO `.cljc` core + `:exec` 実行分離 +
renderer-mac ffmpeg 合成）ベース。設計の正本: superproject ADR-2607165000。

このチャンネルは同時に **kotoba-lang 3D sim 系（nagare / kudaki / fea /
cae-solver / aero / echem / vphysics / num）の性能・適用範囲を実地で試す
ベンチ台**でもある — 全 sim 実行は benchmark datom を append-only 台帳に残す。

## 中心不変条件 — LLM に算術をさせない

AI は計算を時々、堂々と間違える。そこで台本中の**すべての数値主張**は
以下 3 種のいずれかの出所（provenance）を機械強制される:

| 出所 | 実体 | 検証 |
|---|---|---|
| `:sim` | kotoba-lang solver の実行結果 datom | 値・単位の一致（相対誤差 ≤2%） |
| `:constant` | `resources/constants.edn`（出典付き定数、人間レビューで追加） | 完全一致 |
| `:citation` | research-brief の出典付き引用 | **人間が verified-by を付けたものだけ** |

さらに publish は決定論 gate 全 green **かつオーナーの明示承認**が要る
（auto-publish しない — 事実確認の最終責任は人間）。

## yukkuri との差分（このリポジトリが所有するもの）

| 層 | yukkuri | dougaka-kagaku |
|---|---|---|
| 台本 | L/R 掛け合い解説（**継承**） | 同フォーマット + 数値主張の provenance 必須 |
| 実験 | なし | **sim stage 新設**（design-sim → run-sim、cae.solver dispatch） |
| 画 | 紙芝居（静止 BG + 立ち絵） | **sim 結果の kami-engine 3D 可視化**（WebGPU/WGSL first、headless render） |
| QA | 尺/ラウドネス advisory | **factcheck HARD gate + 人間レビュー無条件 hold** |
| 公開 | 手動 | madeForKids=false・通常広告（幼児より高 CPM） |

## 構成

- `src/kagaku/scenario.cljc` — episode spec（問い + claims + sim-cases + beats）の
  検証。数値主張の provenance 必須ルールはここ。純データ、外部 IO なし。
- `src/kagaku/simcase.cljc` — sim case EDN の検証（solver kind / 規模 budget）、
  実行結果と claim の整合チェック、benchmark datom 化。
- `src/kagaku/factcheck.cljc` — fact-check gate。決定論チェックのみ
  （provenance / sim 一致 / 定数出典 / シミュレーション明示 / metadata /
  尺 / loudness）+ 人間承認必須の publish 判定。
- `src/kagaku/pipeline.cljc` — produce stage-order と advance reducer
  （yukkuri `graphs/produce.cljc` と同型。`:factcheck` と `:human-review` が gate）。
- `resources/series.edn` — 5 series の topic カタログ（月接近 / 動物比較 /
  身近な仕組み / 未来技術 / 3分数学・物理）。
- `resources/constants.edn` — 出典付き物理定数（:constant 出所の唯一のテーブル）。
- `content/tsuki-half-distance.edn` — episode spec 実例。

## 実行

```bash
# テスト（nbb が第一経路。JVM は互換）
nbb --classpath src:test test/run.cljs
clojure -M:test

# episode 横断 self-audit（全 content/*.edn の健全性を機械検査、CI ゲート）
nbb --classpath src tools/audit.cljs

# 1 episode を full-produce E2E（compose→…→render-video の純データ経路を畳む）
nbb --classpath src tools/run_episode.cljs content/tsuki-half-distance.edn

# 次に作る topic を選定（priorityScore + series ローテーション）
nbb --classpath src tools/next_topic.cljs
```

CI（`.github/workflows/ci.yml`）は push / PR で上記の nbb テスト + self-audit を
回し、episode 追加時の回帰を機械保証する。

実 IO（solver 実行 / VOICEVOX / ComfyUI / YouTube / D1）はこのリポジトリに
置かない。yukkuri と同じく実行系は `:exec` 側（cae.solver hosts / murakumo
engines / kami-engine headless / renderer-mac）が担う。

## 誠実性 / credits

- 実験映像は**全編シミュレーション**であることを動画内と description で明示する
  （実写実験と誤認させない — factcheck gate の必須チェック）。
- 音声に VOICEVOX を使う場合、公開 description に `VOICEVOX:<話者名>`
  クレジット必須（yukkuri と同一の不変条件）。
- 3D は repo-wide 規則どおり kami-engine stack のみ（Three.js 等の第 2 エンジン
  禁止、DOM/SVG/CSS3D の疑似 3D 禁止）。
