# 開発者向け資料

バージョンの付け方と過去の開発版の対応は[バージョン規則](VERSIONING.md)、未リリース機能の設定方法は[開発版の設定](DEVELOPMENT_SETTINGS.md)を参照してください。現在の開発版は`1.1.0-dev.7`です。

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

生成先は`build/private-dist/RTMClientSway-1.1.0-dev.7.jar`です。開発版JARはローカルでのみ保管し、Gitへの追加や公開Actionsアーティファクトへのアップロードは行いません。正式版だけを検証後にGitHub Releasesで配布します。
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

動揺モデルはJavaで実行し、回転中心を1.15mとします。走行倍率のDataMap補正は読み取り専用です。車両生成時に補正値を取得し、更新間隔が大きく空いた場合はばね状態を保持して微分入力を再初期化します。partialTickが100Tickにわたり0のままの場合は時計による補間へ切り替えます。

`verify.ps1`は描画バイトコード検証に加え、`VerifyMotion.java`の数値テストを実行します。描画負荷・外傾方向・分岐衝撃位置・Angelica併用を含むゲーム内確認は別途必要です。

## 詳細設定

Configに52個の数値項目、9個のノッチ倍率、DataMapキー、効果別の有効・無効を定義します。設定の読み込みと検証は車両状態の生成前に行います。ばね係数と分岐衝撃係数は生成時にキャッシュします。

`VerifyTuningConfig` はForgeの設定クラスを用いて、移行バックアップ、保存済み数値の保持、効果別スイッチの保存と動揺の抑止を確認します。`TuningParser` は比較試験用のtools内に配置しています。

## 参考元との数値比較

`tools/reference/RTMBodyMotion.js`は固定コミットの参考元JSを保管したテスト用資料です。Java 8 NashornでそのJSを実行し、乗客荷重OFF・同じ入力・同じ補間係数でJavaの状態と姿勢を比較します。本番JARには含めません。

`VerifyReference`は前進・後退、直線、カント付き曲線、S字、トング・クロッシング衝撃、制動・停止、DataMap補正（初期値を含む）、3Tickの更新、長い更新飛び、瞬間移動・大きなYaw変化、既定値と変更済み設定を含む152万件を比較します。最大差1e-9以下を合格条件とし、dev.7の実測最大差は約1.29e-14でした。実機のAPI入力・分岐位置・描画結果や時計補間はゲーム内試験で確認します。

## 参考資料

- [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX)
- [READMEへ戻る](../README.md)
