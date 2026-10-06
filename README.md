# RTM Client Sway

Minecraft 1.7.10 / KaizPatchX向けの、列車の車体に揺れを追加するクライアント専用MODです。

[ダウンロード](https://github.com/hachiko-tokkai/RTMClientSway/releases/latest) · [不具合報告](https://github.com/hachiko-tokkai/RTMClientSway/issues)

## 概要

RTM（RealTrainMod）の列車に、走行中の上下・左右の揺れ、カーブや分岐での傾き、停車時の揺り返しを追加します。

揺れは導入したプレイヤーの画面にのみ反映されます。車両の速度・加減速性能・当たり判定は変更しません。サーバーへの導入は不要です。

揺れのON/OFFと強さは設定から調整できます。

## 対応環境

| 必要なもの | バージョン |
|---|---|
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 |
| KaizPatchX | 1.10.1以上 |

本MODはKaizPatchX 1.10.1以上の環境を前提としています。別バージョンのMinecraft、KaizPatchX 1.10.1未満、および公式RTM環境は未検証です。

v1.0.1からAngelica 2.2.29以上に対応しています。

## 注意事項

- RTMの標準的な列車描画経路を使う車両が対象です。全追加パックでの動作を保証するものではありません。
- ほかの描画変更MODとの組み合わせでは、不具合が発生する場合があります。

## 導入方法

1. [リリースページ](https://github.com/hachiko-tokkai/RTMClientSway/releases/latest)から最新版のJARをダウンロードします。
2. KaizPatchXを使用している起動構成の`mods`フォルダーへ入れます。
3. Minecraftを起動します。

同じMODの古いJARがある場合は、取り除いてから新しいJARを入れてください。

## 主な機能

開発版1.1.0-devでは、[RTMBodyMotion](https://github.com/C-TREC/RTMBodyMotion)（C-TREC & 月島重工）の動揺計算をJavaへ移植しています。曲線のカント不足・進入退出の衝撃、速度に応じた確率的な走行動揺、各台車のトングレール・クロッシング通過衝撃、制動・停止衝動を、固有周波数と減衰を持つばねで処理します。乗客荷重機能は含みません。この開発版のゲーム内動作は未確認です。

| 場面 | 車体の動き |
|---|---|
| 走行中 | 速度と走行距離に応じた上下・左右の揺れと傾き |
| カーブ | 速度、台車の向き、線路の傾きに応じた追加の傾き |
| 分岐 | 分岐方向に応じた傾きと横方向の衝撃 |
| 停車 | 停止直前の減速の強さと変化に応じた揺り返し |

前進・後退の両方が対象です。車体と一緒に方向幕や発光部も動きます。

## 設定

タイトル画面の「Mods → RTM Client Sway → Config」から変更できます。

開発版1.1.0-devのConfig画面は「基本設定」「curve / 曲線」「straight / 走行」「turnout / 分岐」「stop / 制動・停止」に分かれています。詳細設定にはPreviewerと同じ項目名・単位を使用し、日本語の説明を表示します。`notchFactors`はN・B1～B7・非常の順の9個です。数値には安全な範囲を設定しており、範囲外のプレビュー設定は読み込みません。

### Previewerの出力を読み込む（開発版）

1. [RTMBodyMotion Previewer](https://github.com/C-TREC/RTMBodyMotionPreviewer/releases/tag/v1.0.0)から`MOTION_TUNING.js`を書き出します。全項目・変更項目のみの両方に対応します。
2. 使用するMinecraft起動構成の`config/rtmclientsway/`フォルダーを作り、そこへファイルを置きます。
3. Config画面の基本設定で`importPreview`をONにして保存します。タイトル画面でも実行できます。
4. 詳細設定を開き直すと、読み込んだ値を確認・変更できます。成功時は`importPreview`が自動でOFFに戻ります。

これは一度だけ取り込む操作です。読み込み後にConfig画面で変更した値は維持され、ファイルが自動で上書きし続けることはありません。再度読み込む場合は`importPreview`をONにします。省略項目は参考元の既定値で補われるため、詳細設定全体を置き換えます。基本設定のON/OFF・倍率・回転中心は維持します。

取り込み前の`rtmclientsway.cfg`は同じフォルダーへ`.before-import-時刻.bak`としてバックアップします。提供ファイル自体は変更しません。失敗時は詳細設定を維持し、理由をログに記録します。`load`・`debug`・未知の項目は未適用としてログに記録します。ファイルはUTF-8の設定データとして解析し、JavaScriptのコードは実行しません。

プレビューとの比較時は各倍率を1にします。回転中心は参考元の1.15 mに合わせることもできます。線路・速度・車両モデルなどの入力が違うため、プレビューとゲーム内の完全一致は保証しません。

| 項目 | 内容 | 初期値 |
|---|---|---|
| `enabled` | 揺れのON/OFF | ON |
| `strength` | 全体の倍率 | 1 |
| `running` | 走行中の揺れの倍率 | 1 |
| `curve` | カーブの傾きの倍率 | 1 |
| `switch` | 分岐の傾き・横衝撃の倍率 | 1 |
| `stop` | 停車時の揺り返しの倍率 | 1 |
| `vertical` | 走行・分岐時の上下動のON/OFF（開発版） | ON |
| `pivotHeight` | 車体が傾くときの回転中心の高さ | 1.5 m |

倍率は0～5です。1が標準、0でその効果を無効にします。`strength`は各場面の倍率にさらに掛かります。

全体を弱くするなら`strength`を下げ、分岐だけ強くするなら`switch`を上げます。
`pivotHeight`は0～4 mです。通常は初期値のままで使用してください。

設定ファイルは`config/rtmclientsway.cfg`です。ファイルを直接編集する場合は、Minecraftを終了してから編集してください。

## 無効化・削除

`enabled`をOFFにすると、本MODによる追加の揺れが無効になります。
削除する場合は`mods`から本MODのJARを取り除いてください。

## 不具合報告

[Issues](https://github.com/hachiko-tokkai/RTMClientSway/issues)に、Minecraft・Forge・KaizPatchXのバージョン、使用した車両パック、再現手順を記載してください。
クラッシュした場合は、クラッシュレポートと`logs/latest.log`も添付してください。

## 開発

ビルド手順、検証方法、描画処理の詳細は[開発者向け資料](docs/DEVELOPMENT.md)を参照してください。

## 生成AIの利用

設計、コード生成・修正、検証用コード、文書作成にOpenAI Codexを使用しています。

## ライセンス・免責事項

本プロジェクトのソースコードとドキュメントには[MIT License](LICENSE)を適用しています。

RTMBodyMotion由来の計算・分岐判定には、参考元の利用条件も適用されます。車体動揺：C-TREC & 月島重工 制作の動揺JS（RTMBodyMotion v1.0）を使用・Java移植しています。詳細は[第三者ソフトウェアの表記](THIRD_PARTY_NOTICES.md)を参照してください。

Copyright (c) 2026 hachiko-tokkai

本MODは現状のまま提供します。動作・互換性・安全性を保証せず、使用に伴う不具合や損害について、作者は適用法令で認められる範囲において責任を負いません。詳細はLICENSEを確認してください。

Minecraft、Forge、KaizPatchXなどの第三者ソフトウェアの権利は、各権利者に帰属します。それらのJARやソースコードは同梱していません。

## 参考資料

- [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX)
- [RTMBodyMotion — C-TREC & 月島重工](https://github.com/C-TREC/RTMBodyMotion)
