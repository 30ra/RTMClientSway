# RTMBodyMotion

車体動揺：C-TREC & 月島重工 制作の動揺JS（RTMBodyMotion v1.0）を参考に、動揺計算と分岐通過判定をJavaへ移植しています。

参照元：https://github.com/C-TREC/RTMBodyMotion
参照コミット：4790b6a49f5a24d64a9c3f850a8c52db5b7a66ab

対象：BodyMotion.java、およびSwayHook.javaの分岐衝撃位置計算。
乗客荷重機能は含みません。車両別JSではなくクライアントMODとして組み込み、既存の設定倍率を適用しています。

以下は参考元の日本語利用条件です。この由来の部分には本リポジトリのMITライセンスだけでなく、元の利用条件も適用されます。

1. 本モジュール（RTMBodyMotion.js、RTMBodyMotionAdapter.js および同梱サンプル）は、自由に使用・改変・再配布できます。
   自作の車両パックに同梱して配布することも可能です。
2. 使用条件：本モジュール（改変版を含む）を使用する車両は、その車両のreadmeに
   「C-TREC & 月島重工 制作の動揺JSを使用」と明記してください。
3. 本モジュールは現状のまま提供され、制作者は使用結果について一切の保証・責任を負いません。

原文：https://github.com/C-TREC/RTMBodyMotion/blob/4790b6a49f5a24d64a9c3f850a8c52db5b7a66ab/ライセンス_License.txt
