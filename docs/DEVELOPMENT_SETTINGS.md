# 開発版の設定

対象バージョンは `1.1.0-dev.7` です。

「Mods → RTM Client Sway → Config」で変更します。設定ファイルは `config/rtmclientsway.cfg` です。保存時に車両の動揺状態を再生成します。

## 効果別の設定

各カテゴリの `enabled` で効果を切り替えます。初期値はすべてONです。

| カテゴリ | 効果 | 主な調整項目 |
|---|---|---|
| sway | 全体の有効・無効 | enabled |
| straight | 走行中の左右・上下動 | defaultScale、rollStdDeg、swayStdM、bounceStdM |
| curve | カント不足による外傾、進入・退出の揺り戻し | amplitudeScale、leanRollDeg、leanSwayM、周波数、減衰比 |
| turnout | トング・クロッシング通過時の衝撃 | toe・frogの衝撃量、bounceFrequencyHz、bounceDamping |
| stop | 急制動・停止時の沈み込みと揺り戻し | pitchImpulse、shiftImpulseM、notchFactors、周波数、減衰比 |

走行と分岐の上下動を個別に調整するには、`straight.bounceStdM` と `turnout.toeBounceImpulse / frogBounceImpulse` を変更します。0でその上下動を無効にできます。

## 設定の移行

旧バージョンのcfgは、移行前に同じディレクトリへバックアップされます。保存済みの詳細な調整値は維持します。プレビュー設定の取り込み項目は整理され、以後の調整はConfig画面またはcfgで行います。

## 計算と検証

RTMBodyMotionの固定コミット `4790b6a49f5a24d64a9c3f850a8c52db5b7a66ab` を基準にしています。カーブ・走行・分岐・停車の入力を、5ms刻みのばね・減衰モデルで計算し、描画時にHermite補間します。回転中心は1.15mです。

ばね係数と分岐衝撃係数は車両状態の生成時に計算し、更新時に再利用します。全効果ONで参考元JSと比較する数値テスト、設定保存・移行・効果無効化のテストを用意しています。描画方向、線路API、Angelica併用などは実機での確認が必要です。

固有周波数と時間倍率の組み合わせによる計算上の周波数は10Hzまでです。上限が0の場合は変位を0として扱います。


