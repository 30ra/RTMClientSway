package rtmsway;

import java.util.*;

/** PreviewerとConfigで共通の設定スキーマ。読み込みは計算開始前に完了する。 */
final class Tuning {
    static final Map<String, Spec> SPECS = new LinkedHashMap<String, Spec>();
    static {
        add("curve", "amplitudeScale", 1, 0, 10, "曲線全体の振幅倍率");
        add("curve", "sluggishnessScale", 1, .05, 20, "応答・余韻の時間倍率。振幅は変更しない");
        add("curve", "maxRollDeg", 3, 0, 20, "合成後の最大横傾き [deg]");
        add("curve", "maxSwayM", .085, 0, 1, "合成後の最大横変位 [m]");
        add("curve", "leanRollDeg", 1.2, 0, 20, "曲線中の持続外傾 [deg]");
        add("curve", "leanSwayM", .030, 0, 1, "曲線中の持続横変位 [m]");
        add("curve", "peakRollDeg", .40, 0, 20, "曲線進入時の追加ロール衝撃 [deg]");
        add("curve", "peakSwayM", .012, 0, 1, "曲線進入時の追加横変位衝撃 [m]");
        add("curve", "saturationMm", 50, .001, 1000, "カント不足の飽和基準 [mm]");
        add("curve", "rollFrequencyHz", .75, .01, 10, "横傾きの固有周波数 [Hz]");
        add("curve", "rollDamping", .10, 0, 1, "横傾きの減衰比");
        add("curve", "swayFrequencyHz", .85, .01, 10, "横変位の固有周波数 [Hz]");
        add("curve", "swayDamping", .10, 0, 1, "横変位の減衰比");
        add("curve", "inputFilterHz", 1.6, .01, 20, "カント不足入力のフィルター [Hz]");
        add("curve", "minSpeedMps", 4, 0, 100, "曲線動揺を開始する最低速度 [m/s]");
        add("curve", "minCantDeficiencyMm", 5, 0, 1000, "曲線動揺を開始する最低カント不足 [mm]");
        add("curve", "exitResponseFactor", .62, 0, 10, "曲線退出時の逆応答倍率");
        add("curve", "reverseExitFactor", 0, 0, 10, "S字反転時の追加退出衝撃倍率");
        integer("curve", "stageThreshold", .018, 0, 10, false, "曲率段階変化のしきい値");
        integer("curve", "stageHoldTicks", 2, 1, 200, true, "段階変化を確認するTick数");
        integer("curve", "exitHoldTicks", 6, 1, 200, true, "曲線退出を確認するTick数");
        add("straight", "defaultScale", .70, 0, 10, "走行動揺の基礎倍率");
        add("straight", "maxScale", 4, 0, 10, "走行動揺倍率の上限");
        add("straight", "rollStdDeg", .12, 0, 10, "走行ロールの標準偏差 [deg]");
        add("straight", "swayStdM", .004, 0, 1, "走行横変位の標準偏差 [m]");
        add("straight", "bounceStdM", .0015, 0, 1, "走行上下動の標準偏差 [m]");
        add("straight", "minSpeedKmh", 3, 0, 360, "走行動揺を開始する最低速度 [km/h]");
        add("straight", "referenceSpeedKmh", 50, .001, 360, "振幅成長の基準速度 [km/h]");
        add("turnout", "minSpeedFactor", .45, 0, 1, "高速時にも残す分岐衝撃の比率");
        add("turnout", "speedFalloffKmh", 60, .001, 360, "分岐衝撃の速度減衰基準 [km/h]");
        add("turnout", "toePeakRollDeg", .20, 0, 20, "トングレールの横傾きピーク [deg]");
        add("turnout", "toePeakSwayM", .005, 0, 1, "トングレールの横変位ピーク [m]");
        add("turnout", "toeBounceImpulse", .038, 0, 5, "トングレールの上下速度衝撃 [m/s]");
        add("turnout", "frogPeakRollDeg", .60, 0, 20, "クロッシングの横傾きピーク [deg]");
        add("turnout", "frogPeakSwayM", .015, 0, 1, "クロッシングの横変位ピーク [m]");
        add("turnout", "frogBounceImpulse", .128, 0, 5, "クロッシングの上下速度衝撃 [m/s]");
        add("turnout", "bounceFrequencyHz", 2.10, .01, 10, "分岐上下動の固有周波数 [Hz]");
        add("turnout", "bounceDamping", .1297, 0, 1, "分岐上下動の減衰比");
        add("stop", "maxPitchDeg", .65, 0, 20, "制動系の最大前後傾き [deg]");
        add("stop", "maxShiftM", .040, 0, 1, "制動系の最大前後変位 [m]");
        add("stop", "emergencyDecelMps2", 1.15, 0, 10, "急制動衝動の減速度しきい値 [m/s²]");
        add("stop", "emergencyJerkMps3", 2, 0, 100, "急制動衝動のジャークしきい値 [m/s³]");
        add("stop", "emergencyPitchImpulse", .0376, 0, 10, "急制動の前後傾き速度衝撃 [deg/s]");
        add("stop", "emergencyShiftImpulseM", .020, 0, 5, "急制動の前後速度衝撃 [m/s]");
        integer("stop", "minimumBrakeLevel", 5, 0, 8, true, "停止衝動の最低ノッチ。8=非常");
        add("stop", "minimumDecelMps2", .75, 0, 10, "停止衝動の最低減速度 [m/s²]");
        add("stop", "pitchImpulse", .098, 0, 10, "停止時の前後傾き速度衝撃 [deg/s]");
        add("stop", "shiftImpulseM", .026, 0, 5, "停止時の前後速度衝撃 [m/s]");
        add("stop", "pitchFrequencyHz", .72, .01, 10, "停止前後傾きの固有周波数 [Hz]");
        add("stop", "pitchDamping", .14, 0, 1, "停止前後傾きの減衰比");
        add("stop", "shiftFrequencyHz", .78, .01, 10, "停止前後変位の固有周波数 [Hz]");
        add("stop", "shiftDamping", .28, 0, 1, "停止前後変位の減衰比");
    }
    final Map<String, Double> values = new LinkedHashMap<String, Double>();
    double[] notch = {0, 0, 0, 0, 0, .50, .75, 1.05, 1.45};
    String dataMapKey = "BodyMotionStraightAdjust";
    Tuning() { for (Spec s : SPECS.values()) values.put(s.path, s.def); }
    double get(String path) { return values.get(path); }
    double rollHz() { return get("curve.rollFrequencyHz") / get("curve.sluggishnessScale"); }
    double swayHz() { return get("curve.swayFrequencyHz") / get("curve.sluggishnessScale"); }
    void checkDynamics() {
        if (rollHz() > 10 || swayHz() > 10)
            throw new IllegalArgumentException("固有周波数/sluggishnessScaleは10Hz以下にしてください。参考元と同じ5ms積分を維持します");
    }
    static final class Spec {
        final String group, key, path, comment;
        final double def, min, max;
        final boolean isInt;
        Spec(String group, String key, double def, double min, double max, boolean isInt, String comment) {
            this.group = group; this.key = key; this.path = group + "." + key;
            this.def = def; this.min = min; this.max = max; this.isInt = isInt; this.comment = comment;
        }
        double check(Object value) {
            if (!(value instanceof Number)) throw new IllegalArgumentException(path + ": 数値ではありません");
            double v = ((Number)value).doubleValue();
            if (!Double.isFinite(v) || v < min || v > max || (isInt && v != Math.rint(v)))
                throw new IllegalArgumentException(path + ": 許容範囲 " + min + "～" + max + (isInt ? "（整数）" : ""));
            return v;
        }
    }
    private static void add(String g, String k, double d, double min, double max, String c) { integer(g, k, d, min, max, false, c); }
    private static void integer(String g, String k, double d, double min, double max, boolean isInt, String c) {
        Spec s = new Spec(g, k, d, min, max, isInt, c); SPECS.put(s.path, s);
    }
    static double[] checkNotch(Object value) {
        if (!(value instanceof List) || ((List<?>)value).size() != 9)
            throw new IllegalArgumentException("stop.notchFactors: N・B1～B7・非常の9個が必要です");
        double[] result = new double[9];
        for (int i = 0; i < 9; i++) {
            Object v = ((List<?>)value).get(i);
            if (!(v instanceof Number) || !Double.isFinite(((Number)v).doubleValue()) || ((Number)v).doubleValue() < 0 || ((Number)v).doubleValue() > 10)
                throw new IllegalArgumentException("stop.notchFactors: 各値は0～10です");
            result[i] = ((Number)v).doubleValue();
        }
        return result;
    }
    void overlay(Map<String, Object> input, List<String> ignored) {
        for (Map.Entry<String, Object> group : input.entrySet()) {
            String name = group.getKey();
            if (name.equals("load") || name.equals("debug")) { ignored.add(name); continue; }
            if (!Arrays.asList("curve", "straight", "turnout", "stop").contains(name)) { ignored.add(name); continue; }
            if (!(group.getValue() instanceof Map)) throw new IllegalArgumentException(name + ": オブジェクトではありません");
            for (Map.Entry<?, ?> e : ((Map<?, ?>)group.getValue()).entrySet()) {
                String path = name + "." + e.getKey();
                if (path.equals("stop.notchFactors")) notch = checkNotch(e.getValue());
                else if (path.equals("straight.dataMapKey")) {
                    if (!(e.getValue() instanceof String) || ((String)e.getValue()).length() > 128)
                        throw new IllegalArgumentException(path + ": 128文字以内の文字列が必要です");
                    dataMapKey = (String)e.getValue();
                }
                else if (SPECS.containsKey(path)) values.put(path, SPECS.get(path).check(e.getValue()));
                else ignored.add(path);
            }
        }
        checkDynamics();
    }
}
