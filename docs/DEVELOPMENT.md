# 開発者向け資料

## ビルドと検証

ビルドにはJava 8のJDK、PowerShell、およびForge 1.7.10開発環境のGradleキャッシュが必要です。ビルドスクリプトは依存ライブラリを自動ダウンロードしません。
キャッシュにはForgeの開発用JAR、ASM 5.0.3、LaunchWrapper 1.12、LWJGL 2.9.1、Guava 17.0が必要です。

リポジトリのルートで実行します。

```powershell
powershell -File build.ps1
```

キャッシュの場所を指定する場合：

```powershell
powershell -File build.ps1 -Cache "D:\gradle-cache"
```

生成先は`dist/RTMClientSway-1.1.0-dev.jar`です。正式版のJARは上書きしません。
対象のKaizPatchX JARへ描画処理を組み込んだバイトコードを検証する場合：

```powershell
powershell -File verify.ps1 -KaizJar "D:\Minecraft\mods\KaizPatchX.jar"
```

この検証はMinecraft上での動作試験を代替するものではありません。

## 描画処理

[KaizPatchXのRenderVehicleBase](https://github.com/Kai-Z-JP/KaizPatchX/blob/master/src/main/java/jp/ngt/rtm/entity/vehicle/RenderVehicleBase.java)の`renderVehicleMain`へ、車体を移動・回転する描画処理を追加します。
車体・ライト・方向幕に同じ変換を適用し、処理後は描画用の行列を元へ戻します。

揺れの計算は車両ごとにTickが変わったときだけ実行し、位置と速度によるHermite補間を行います。分岐判定は台車の現在レールを優先し、取得できない場合のみ座標照会します。交点近似はPoint単位でキャッシュし、通過判定は前回と今回の台車座標を結ぶ線分を用います。
車両ごとの状態はクライアント内に保存します。APIの不一致を検出した場合はエラーをログに記録し、追加の揺れを停止します。

## 開発版の動揺モデル

RTMBodyMotion v1.0（C-TREC & 月島重工）の曲線イベント、ばね積分、確率的走行入力、分岐衝撃位置計算、停止衝動、Hermite補間をJavaへ移植しています。参照コミットと利用条件は[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)を参照してください。

- 曲線：速度と車体Yawの変化から曲率を求め、カント不足を入力にします。符号の安定確認、段階変化の2Tick確認、退出の6Tick確認、入力衝撃の上限、S字の切り替えを維持しています。
- 走行：速度依存のガウス入力を各ばねの速度へ加算します。旧版の走行距離による周期波とは異なります。
- 分岐：前後台車のトングレール・クロッシング通過を独立検出します。横傾き・横変位・上下衝撃を加えます。位置を97点ずつ標本化する交点近似であり、実物の軌道部品を直接検出するものではありません。
- 停止：減速度とジャークによる制動衝動、およびB5以上・減速度0.75m/s²以上の停止衝動を分離します。低ノッチでは停止衝撃を加えません。
- ばね：各チャンネルの固有周波数・減衰・上限を維持し、最大5msの小刻みな時間積分と補間を用います。

参考元との差分は、乗客荷重を省くこと、既存のクライアント設定倍率と回転中心を維持すること、RTM描画から受け取るpartialTickを使うことです。参考元のJSを直接実行する方式ではありません。走行倍率のDataMap補正は指定キーを読み取るだけで、書き込みやパケット送信を行いません。

`verify.ps1`は描画バイトコード検証に加え、`VerifyMotion.java`の数値テストを実行します。描画負荷・外傾方向・分岐衝撃位置・Angelica併用を含むゲーム内確認は別途必要です。

## 詳細設定・Previewer互換

`Tuning`に52個の数値項目・9個のノッチ倍率・DataMapキーを定義し、ConfigとPreviewerファイルを同じスキーマで検証します。`TuningParser`はオブジェクト、配列、文字列、有限数、真偽値、null、コメントだけを解析する限定パーサーであり、スクリプトエンジンは使用しません。256KiB・入れ子16段の上限を設けています。

`TuningConfig`は全項目を検証してからバックアップ・反映します。省略項目は既定値で補い、読み込み操作は成功時にOFFへ戻します。設定保存時に車両状態を初期化するため、異なる固有周波数の古い状態は持ち越しません。通常の描画・Tick処理中にファイルを読みません。

ゼロ振幅上限、減衰0・1にも対応します。極端な高周波設定では数値積分の時間刻みをさらに小さくして発散を防止します。既定値では参考元と同じ5ms刻みです。

`VerifyTuning`は実際のエクスポート、部分設定、式・コードの拒否、範囲・配列・重複キー検証、設定値の動揺反映を確認します。`VerifyTuningConfig`はForgeの設定クラスで保存、再読み込み、整数項目、バックアップ、失敗時の未反映を確認します。

```powershell
powershell -File verify.ps1 -KaizJar "D:\Minecraft\mods\KaizPatchX.jar" -TuningFile "D:\Downloads\MOTION_TUNING.js"
```

## 参考資料

- [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX)
- [READMEへ戻る](../README.md)
