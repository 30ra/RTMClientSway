# 開発版の設定

現在の開発版は `1.1.0-dev.6` です。JARはローカルにのみ保管し、公開READMEには正式リリース済みの説明だけを記載します。

## 設定画面

「Mods → RTM Client Sway → Config」から変更します。基本設定には全体のON/OFFとPreviewer取り込み操作だけを残し、動揺の調整は参考元と同じ `curve / straight / turnout / stop` の項目に統一しています。

MOD独自の `strength / running / curve / switch / stop` 倍率、`vertical`、`pivotHeight` は廃止しました。旧cfgにあっても適用しません。設定を整理する前に `.before-reference-時刻.bak` を作成します。回転中心は参考元の1.15 mに固定です。

- 走行時の揺れ：`straight.defaultScale` または `rollStdDeg / swayStdM / bounceStdM`。
- カーブの揺れ：`curve.amplitudeScale`、持続外傾や進入衝撃の設定。
- 分岐：`turnout.toe*` と `turnout.frog*` の設定。
- 制動・停止：`stop` の各衝撃量・周波数・減衰。
- 上下動を不要にする場合：`straight.bounceStdM` と `turnout.toeBounceImpulse / frogBounceImpulse` を0にします。

設定ファイルは `config/rtmclientsway.cfg` です。共通の詳細設定は保存済みの値を維持します。独自倍率だけを除去する変更であり、既に調整した詳細項目を自動で既定値へ戻しません。

## Previewer取り込み

1. [Previewer](https://github.com/C-TREC/RTMBodyMotionPreviewer/releases/tag/v1.0.0)から `MOTION_TUNING.js` を書き出します。
2. 使用する起動構成の `config/rtmclientsway/` へ置きます。
3. 基本設定の `importPreview` をONにして保存します。
4. 成功するとOFFに戻ります。詳細設定を開き直して値を確認できます。

取り込みは一度だけです。共通の詳細設定を置き換え、省略項目は参考元の既定値を使用します。取り込み前にcfgをバックアップします。失敗したファイルは適用しません。JavaScriptのコードは実行せず、設定データとして解析します。乗客荷重の `load`、描画試験用の `debug` は適用しません。

## 参考元との比較

参照コミットは `4790b6a49f5a24d64a9c3f850a8c52db5b7a66ab` です。乗客荷重を無効にし、同じ入力と補間係数を与えたJSとJavaの比較テストを行います。乱数シード、符号付き速度による加速度、ばねの5ms積分、曲線イベント、分岐衝撃、停止衝動、Hermite補間を合わせています。

これは全パック・実機描画の完全一致を保証するものではありません。実際の線路形状・台車情報・描画タイミングによる確認は別途必要です。さらに、0の描画上限ではNaNを避ける安全処理を残し、固有周波数/sluggishnessScaleが10Hzを超える設定は異なる積分に置き換えず拒否します。

## 全車両共通

車両別設定はdev.5で廃止済みです。旧cfgの `vehicles` は使用せず、既存データは削除しません。ZIP単位の設定とRTMAddonPackChecker連携はありません。


