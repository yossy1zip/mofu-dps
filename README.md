# MofuDPS

Paper 1.21.8 向けプラグイン。プレイヤーがMob(プレイヤー以外)に与えたダメージからリアルタイムDPSを計測し、PlaceholderAPI・チャットコマンドで参照できます。複数のMobに与えたダメージは合算されます。

> 実運用のプラグインから、サーバーの設定・秘密を抜いた**公開用の写し**です(履歴は 1 コミット)。`mvn clean package` でビルドと単体テストが通ります(テスト 5 件。DPS の集計ウィンドウ)。CI の見本は `docs/ci.example.yml`。

## 作った理由 / 自分がしたこと / AI に任せたこと
<!-- 下書き: 内容は本人が確認して直す(コードと過去の発言から起こした) -->

- **なぜ**: PvE のボス戦で「どれだけ強い攻撃ができたか」を、ゲーム内の看板・チャットと、公式サイト(別リポジトリ `mofucraft-site`)のランキングの両方で見せたかった。
- **自分が決めたこと**: 計測の仕様(直近 5 秒の平均・過去最大・複数 Mob の合算)/ ランキングを MySQL に残し、公式サイトから**読み取り専用**で参照する設計 / 導入と運用。
- **AI に任せたこと**: 実装の大部分(AI コーディングエージェントに指示して書かせ、サーバーで確認)。単体テストは公開用の写しを作るときに追加した。

## 特徴

- 直近5秒平均DPS / 過去最大DPS をリアルタイム計測
- PlaceholderAPI 連携(未導入でも動作します)
- `/mofudps measure` によるカウントダウン付きの任意区間DPS測定(Placeholderとは独立)
- チャットでのDPSランキング表示、ランキングのリセット(全体/プレイヤー単位)
- ランキングの永続化先は SQLite(デフォルト) / MySQL を切り替え可能。MySQLを使えば外部のWebアプリ(Remixなど)から同じデータベースへ直接クエリしてランキングを参照できます
- メッセージ文言は `config.yml` から編集可能

## 動作環境

- Paper 1.21.8 (Java 21)
- PlaceholderAPI (任意)

## ビルド

```sh
mvn -DskipTests clean package
```

`target/MofuDPS-<version>.jar` を `plugins/` に配置してください。JDBCドライバ(SQLite/MySQL)は同梱済みのため追加のライブラリ導入は不要です。

## コマンド

| コマンド | 権限 | 説明 |
|---|---|---|
| `/mofudps measure <秒数>` | `mofudps.measure` (default: true) | 5秒カウントダウン後、指定秒数の平均DPSを計測 |
| `/mofudps ranking [件数]` | `mofudps.ranking` (default: true) | DPSランキングをチャットに表示 |
| `/mofudps reset all confirm` | `mofudps.reset` (default: op) | 全プレイヤーのランキング記録をリセット |
| `/mofudps reset <プレイヤー> confirm` | `mofudps.reset` (default: op) | 指定プレイヤーの記録をリセット |
| `/mofudps reload` | `mofudps.reload` (default: op) | config.yml を再読み込み |

エイリアス: `/mdps`

## PlaceholderAPI

| Placeholder | 説明 |
|---|---|
| `%mofudps_dps%` | 直近5秒平均DPS |
| `%mofudps_maxdps%` | 過去最大DPS |
| `%mofudps_rank%` | 自分の現在の順位 |
| `%mofudps_rank_<N>_name%` | ランキングN位のプレイヤー名 |
| `%mofudps_rank_<N>_dps%` | ランキングN位のDPS値 |

## 設定

`config.yml` でストレージ(SQLite/MySQL)、ランキング表示件数、集計ウィンドウ秒数、各種メッセージ文言を設定できます。詳細はファイル内のコメントを参照してください。
