package rtmsway;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

final class TuningConfig {
    private static final Logger LOG = Logger.getLogger("RTMClientSway");
    static Tuning load(Configuration config, File configDir) {
        Tuning result = new Tuning();
        for (Tuning.Spec s : Tuning.SPECS.values()) {
            Property p = property(config, s);
            try { result.values.put(s.path, s.check(p.getDouble(s.def))); }
            catch (IllegalArgumentException e) {
                LOG.warning(e.getMessage() + "; 既定値へ戻します");
                if (s.isInt) p.set((int)s.def); else p.set(s.def);
            }
        }
        try { result.checkDynamics(); }
        catch (IllegalArgumentException e) {
            LOG.warning(e.getMessage() + "; 周波数と時間倍率を既定値へ戻します");
            for (String key : new String[]{"curve.rollFrequencyHz", "curve.swayFrequencyHz", "curve.sluggishnessScale"}) {
                Tuning.Spec s = Tuning.SPECS.get(key); result.values.put(key, s.def); property(config, s).set(s.def);
            }
        }
        Property notch = config.get("stop", "notchFactors", result.notch, "N,B1,B2,B3,B4,B5,B6,B7,非常の順。9個の倍率 [0～10]", 0, 10, true, 9);
        try {
            List<Double> values = new ArrayList<Double>(); for (double n : notch.getDoubleList()) values.add(n);
            result.notch = Tuning.checkNotch(values);
        } catch (IllegalArgumentException e) { LOG.warning(e.getMessage()); notch.set(result.notch); }
        result.dataMapKey = config.get("straight", "dataMapKey", "BodyMotionStraightAdjust", "走行倍率の補正値を読むDataMapキー。読み取りのみ。空文字で無効").getString();
        Property isImport = config.get("sway", "importPreview", false,
            "ONにして保存するとconfig/rtmclientsway/MOTION_TUNING.jsを一度だけ取り込みます。成功後はOFFに戻ります。詳細設定を置き換え、省略項目は参考元の既定値になります。以後はConfig画面で調整できます。");
        if (isImport.getBoolean(false)) {
            Path source = new File(new File(configDir, "rtmclientsway"), "MOTION_TUNING.js").toPath();
            try {
                if (Files.size(source) > 262144) throw new IOException("ファイルの上限は256KiBです");
                String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
                Tuning imported = new Tuning(); List<String> ignored = new ArrayList<String>();
                imported.overlay(TuningParser.parse(text), ignored);
                // 完全な解析・範囲検証が済んでから、既存cfgをバックアップして一括反映する。
                Path old = new File(configDir, "rtmclientsway.cfg").toPath();
                if (Files.exists(old)) Files.copy(old, old.resolveSibling("rtmclientsway.cfg.before-import-" + System.currentTimeMillis() + ".bak"));
                for (Tuning.Spec s : Tuning.SPECS.values()) {
                    Property p = property(config, s);
                    if (s.isInt) p.set((int)imported.get(s.path)); else p.set(imported.get(s.path));
                }
                notch.set(imported.notch);
                config.get("straight", "dataMapKey", "BodyMotionStraightAdjust").set(imported.dataMapKey);
                result = imported; isImport.set(false);
                LOG.info("Previewer設定を読み込みました: " + source + "; 未適用項目=" + ignored);
            } catch (IOException | IllegalArgumentException e) {
                LOG.warning("Previewer設定を適用しませんでした。現在のConfigを維持します: " + source + "; " + e.getMessage());
            }
        }
        return result;
    }
    private static Property property(Configuration config, Tuning.Spec s) {
        return s.isInt ? config.get(s.group, s.key, (int)s.def, s.comment, (int)s.min, (int)s.max)
            : config.get(s.group, s.key, s.def, s.comment, s.min, s.max);
    }
}
