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

開発版1.1.1-devでは、[RTMBodyMotion](https://github.com/C-TREC/RTMBodyMotion)（C-TREC & 月島重工）の動揺計算をJavaへ移植しています。曲線のカント不足・進入退出の衝撃、速度に応じた確率的な走行動揺、各台車のトングレール・クロッシング通過衝撃、制動・停止衝動を、固有周波数と減衰を持つばねで処理します。乗客荷重機能は含みません。この開発版のゲーム内動作は未確認です。

| 場面 | 車体の動き |
|---|---|
| 走行中 | 速度と走行距離に応じた上下・左右の揺れと傾き |
| カーブ | 速度、台車の向き、線路の傾きに応じた追加の傾き |
| 分岐 | 分岐方向に応じた傾きと横方向の衝撃 |
| 停車 | 停止直前の減速の強さと変化に応じた揺り返し |

前進・後退の両方が対象です。車体と一緒に方向幕や発光部も動きます。

## 設定

タイトル画面の「Mods → RTM Client Sway → Config」から変更できます。

開発版1.1.1-devのConfig画面は「基本設定」「curve / 曲線」「straight / 走行」「turnout / 分岐」「stop / 制動・停止」「vehicles / 車両別設定」に分かれています。詳細設定にはPreviewerと同じ項目名・単位を使用し、日本語の説明を表示します。`notchFactors`はN・B1～B7・非常の順の9個です。数値には安全な範囲を設定しており、範囲外のプレビュー設定は読み込みません。

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
| `pivotHeight` | 車体が傾くときの回転中心の高さ | 安定版1.5 m／開発版1.15 m（保存済み値は維持） |

倍率は0～5です。1が標準、0でその効果を無効にします。`strength`は各場面の倍率にさらに掛かります。

全体を弱くするなら`strength`を下げ、分岐だけ強くするなら`switch`を上げます。
`pivotHeight`は0～4 mです。通常は初期値のままで使用してください。

設定ファイルは`config/rtmclientsway.cfg`です。ファイルを直接編集する場合は、Minecraftを終了してから編集してください。

## 車両別設定（1.1.1-dev）

現在の開発版は`1.1.1-dev`です。車両別設定と描画上限の修正を含みます。MOD内のバージョン表記とJAR名を揃えています。

ゲーム内のMOD設定にある`vehicles`から車両別に調整できます。車両を表示した後に設定画面を開くと、そのモデル名の設定が追加されます。未検出の車両は`models`に車両JSONのモデル名を登録して保存し、設定画面を開き直してください。表示名ではなく、KaizPatchXの`getModelName()`が返す名前の完全一致です。先頭車・中間車などモデル名が異なる場合は、それぞれ設定します。

- 車両別の項目は空欄が初期値で、共通設定を継承します。共通設定を変更すると継承先も更新されます。
- 数値を入力した項目だけを上書きします。ON/OFFは`enabled`・`vertical`に`true`または`false`を入力します。共通OFFでも車両別ONを指定できます。
- 空欄に戻すと共通設定へ戻ります。車両別カテゴリの既定値リセットで全項目を継承できます。不正な値はログに記録し、その項目は共通設定を使います。
- `stop.notchFactors`は9個の数値をカンマ区切りで入力します。`straight.dataMapKey`は空欄で継承、`-`で補正を無効にします。
- Previewerの出力を`config/rtmclientsway/`に置き、その車両の`previewFile`にファイル名を指定して`importPreview`をONにし保存すると、一度だけその車両へ取り込みます。省略項目は変更しません。成功後はOFFに戻り、取り込み前のcfgをバックアップします。不正なファイルは一括で拒否します。

車両別設定は共通設定と同じ計算に渡されます。車両名の確認はTick単位、ファイル読み込みは設定時だけで、サーバーやDataMapへの書き込みはありません。モデル変更時や設定再読み込み時には動揺履歴を初期化します。

開発版では全体倍率を最大変位・最大角度の制限前に適用し、倍率を上げても設定上限を超えないよう修正しています。既存の倍率・回転中心の保存値は自動変更しません。参考元との全入力に対する数値的一致、ゲーム内のGUI・描画は未検証です。

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
