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

生成先は`dist/RTMClientSway-1.0.0.jar`です。
対象のKaizPatchX JARへ描画処理を組み込んだバイトコードを検証する場合：

```powershell
powershell -File verify.ps1 -KaizJar "D:\Minecraft\mods\KaizPatchX.jar"
```

この検証はMinecraft上での動作試験を代替するものではありません。

## 描画処理

[KaizPatchXのRenderVehicleBase](https://github.com/Kai-Z-JP/KaizPatchX/blob/master/src/main/java/jp/ngt/rtm/entity/vehicle/RenderVehicleBase.java)の`renderVehicleMain`へ、車体を移動・回転する描画処理を追加します。
車体・ライト・方向幕に同じ変換を適用し、処理後は描画用の行列を元へ戻します。

揺れの計算は車両ごとにTickが変わったときだけ実行し、描画フレームの間を補間します。分岐判定のレール照会は更新ごとに前後の台車で各1回です。
車両ごとの状態はクライアント内に保存します。APIの不一致を検出した場合はエラーをログに記録し、追加の揺れを停止します。

## 参考資料

- [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX)
- [READMEへ戻る](../README.md)
