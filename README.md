# RTM Client Sway

Minecraft 1.7.10 / Forge / KaizPatchX向けのクライアント専用車体揺れMODです。
`dist/RTMClientSway-1.0.0.jar`をクライアントの`mods`へ入れます。サーバーへの導入は不要です。

## 対応環境と確認状況

- Minecraft 1.7.10 / Forge 10.13.4.1614
- KaizPatchX 1.10.1の描画クラスへの組み込みを静的検証済み
- Java 8でビルド済み

Minecraft上の描画・設定画面・シェーダーとの組み合わせは未検証です。
RTMの標準的な列車描画経路を使う車両が対象です。全追加パックでの動作は保証していません。

## 設定

Minecraftの「Mods → RTM Client Sway → Config」で設定できます。
ゲームを終了せず、保存後の描画から反映します。設定変更時は揺れの履歴を初期化します。
ファイルは`config/rtmclientsway.cfg`です。

| 設定 | 内容 |
|---|---|
| enabled | 全体のON/OFF |
| strength | 全体倍率、0～5、初期値1 |
| running | 走行時倍率、0～5 |
| curve | 曲線時倍率、0～5 |
| switch | 分岐時倍率、0～5 |
| stop | 停車時倍率、0～5 |
| vertical | 走行・停車時の上下動ON/OFF |
| pivotHeight | 回転中心高さ、初期値1.5m |

台車間距離は各車両のTrainConfig.getBogiePos()から取得します。
前後どちらの走行でも絶対速度から判定します。走行中は走行距離を入力とした波形、曲線は台車角度・速度・カント、分岐は実際の分岐レール、停車時は減速度・ジャークから計算します。
分岐で上下の衝撃は追加しません。台車自体は追加揺動せず、車体・方向幕・発光部へ同じ変換を適用します。
物理挙動、車両の位置、速度、サーバー側処理、カメラの位置は変更しません。

## 既存スクリプトとの関係

MODをOFFにするか取り外すと、追加の揺れは無くなります。
ほかのパックに独自の揺動処理がある場合は重複適用されます。その場合、そのパックの揺動呼び出しを無効化する必要があります。

## 復元

MODを取り外すとMOD由来の揺れが無くなります。

## ビルド・検証

Java 8のJDKとForge 1.7.10開発キャッシュが必要です。このリポジトリだけでは依存ライブラリを自動ダウンロードしません。

```powershell
powershell -File build.ps1
# 別のキャッシュを指定する場合
powershell -File build.ps1 -Cache "D:\gradle-cache"
# 対象KaizPatchXのバイトコード検証
powershell -File verify.ps1 -KaizJar "D:\Minecraft\mods\KaizPatchX.jar"
```

生成先は`dist/RTMClientSway-1.0.0.jar`です。
Minecraftへ直接リンクするメソッド呼び出しを避け、MCP/SRG両フィールド名に対応することで手動ビルドの再難読化を不要にしています。
ASMは実行環境のものを使用し、同梱しません。
実際に導入されているKaizPatchX 1.10.1の描画クラスを対象に、ASM BasicVerifierによるバイトコード検証を行っています。
Minecraftを起動した描画・設定画面・シェーダーとの組み合わせは未検証です。

## 実装の根拠

参照ソース: [KaizPatchX / RenderVehicleBase.java](https://github.com/Kai-Z-JP/KaizPatchX/blob/master/src/main/java/jp/ngt/rtm/entity/vehicle/RenderVehicleBase.java)
`renderVehicleBase`が車両の位置・Yaw/Pitch/Roll・モデルoffsetを適用した後、`renderVehicleMain`で車体・ライト・方向幕を描画します。
`renderVehicleMain`をGL行列のpush/pop付きで囲み、例外発生時も追加分をpopします。
更新はEntityのticksExistedが変わったときだけで、描画パスや材質の数による多重更新はありません。
台車のレール照会は更新時に2回だけです。反射APIは一度解決したメンバーをキャッシュし、揺動状態はWeakHashMapに保存します。
API不一致時はログを1回記録して揺動を停止します。

## 生成AIの利用について

本MODはOpenAI Codexを使用して開発しています。設計、ソースコードの生成・修正、検証用コードおよびドキュメント作成に生成AIを使用しました。
ビルドと静的検証は実施していますが、Minecraft上での動作試験は未実施です。

## ライセンス

本プロジェクトのソースコードとドキュメントは[MIT License](LICENSE)で公開しています。

Copyright (c) 2026 hachiko-tokkai

Minecraft、Minecraft Forge、KaizPatchX、LWJGL、ASMなどの第三者ソフトウェアの権利は、それぞれの権利者に帰属します。
それらのJARやソースコードはこのリポジトリおよび配布JARには同梱していません。

## 免責事項

本MODは現状のまま提供され、動作、互換性、安全性、特定目的への適合性を保証しません。
本MODの使用に伴う不具合、データ損失その他の損害について、作者は適用法令で認められる範囲において責任を負いません。
詳しくは[LICENSE](LICENSE)を確認してください。
