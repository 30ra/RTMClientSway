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
| KaizPatchX | 1.10.1 |

本MODはKaizPatchX1.10.1環境を前提としています。別バージョンのMinecraft・KaizPatchX、および公式RTM環境は未検証です。

## 注意事項

- 独自の車体揺れを持つ車両パックでは、揺れが重複します。本MODまたはパック側の揺動処理を無効にしてください。
- RTMの標準的な列車描画経路を使う車両が対象です。全追加パックでの動作を保証するものではありません。
- ほかの描画変更MODとの組み合わせでは、不具合が発生する場合があります。

## 導入方法

1. [リリースページ](https://github.com/hachiko-tokkai/RTMClientSway/releases/latest)の「Assets」から最新版のJARをダウンロードします。
2. KaizPatchXを使用している起動構成の`mods`フォルダーへ入れます。
3. Minecraftを起動します。

同じMODの古いJARがある場合は、取り除いてから新しいJARを入れてください。

## 主な機能

| 場面 | 車体の動き |
|---|---|
| 走行中 | 速度と走行距離に応じた上下・左右の揺れと傾き |
| カーブ | 速度、台車の向き、線路の傾きに応じた追加の傾き |
| 分岐 | 分岐方向に応じた傾きと横方向の衝撃 |
| 停車 | 停止直前の減速の強さと変化に応じた揺り返し |

前進・後退の両方が対象です。車体と一緒に方向幕や発光部も動きます。
分岐で上下の衝撃は追加しません。台車自体の追加揺動と、乗車中のカメラを揺らす機能はありません。

## 設定

タイトル画面の「Mods → RTM Client Sway → Config」から変更できます。
設定画面で保存した変更は、再起動せずに反映する実装です。

| 項目 | 内容 | 初期値 |
|---|---|---|
| `enabled` | 揺れのON/OFF | ON |
| `strength` | 全体の倍率 | 1 |
| `running` | 走行中の揺れの倍率 | 1 |
| `curve` | カーブの傾きの倍率 | 1 |
| `switch` | 分岐の傾き・横衝撃の倍率 | 1 |
| `stop` | 停車時の揺り返しの倍率 | 1 |
| `vertical` | 走行・停車時の上下動のON/OFF | ON |
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
Java 8でのビルドと、KaizPatchX 1.10.1の描画クラスへの組み込みの静的検証を実施済みです。Minecraft上での動作試験は未実施です。

## ライセンス・免責事項

本プロジェクトのソースコードとドキュメントには[MIT License](LICENSE)を適用しています。

Copyright (c) 2026 hachiko-tokkai

本MODは現状のまま提供します。動作・互換性・安全性を保証せず、使用に伴う不具合や損害について、作者は適用法令で認められる範囲において責任を負いません。詳細はLICENSEを確認してください。

Minecraft、Forge、KaizPatchXなどの第三者ソフトウェアの権利は、各権利者に帰属します。それらのJARやソースコードは同梱していません。

## 参考資料

- [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX)
