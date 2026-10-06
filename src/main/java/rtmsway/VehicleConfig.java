package rtmsway;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import net.minecraftforge.common.config.Configuration;

/** モデル名の完全一致。描画中のファイルI/O・パケット送信は行わない。 */
final class VehicleConfig {
    private static final Logger LOG = Logger.getLogger("RTMClientSway");
    static final Set<String> seen = new TreeSet<String>();
    private static final Set<String> offered = new HashSet<String>();
    static final Map<String, Profile> profiles = new HashMap<String, Profile>();
    static Profile common;
    static final String[] BASE = {"strength", "running", "curve", "switch", "stop", "pivotHeight"};

    static final class Profile {
        Tuning tuning;
        boolean isOn, isVertical;
        double gain, run, curve, branch, stop, pivot;
        Profile(Tuning tuning) {
            this.tuning = copy(tuning);
            isOn = SwayMod.isOn; isVertical = SwayMod.isVertical;
            gain = SwayMod.gain; run = SwayMod.run; curve = SwayMod.curve;
            branch = SwayMod.branch; stop = SwayMod.stop; pivot = SwayMod.pivot;
        }
    }
    static Tuning copy(Tuning source) {
        Tuning t = new Tuning(); t.values.putAll(source.values);
        t.notch = source.notch.clone(); t.dataMapKey = source.dataMapKey; return t;
    }
    static String category(String model) {
        // UTF-8の全バイトを符号化し、ドット・日本語・ハッシュ衝突を避ける。
        StringBuilder id = new StringBuilder("vehicles.m");
        for (byte b : model.getBytes(StandardCharsets.UTF_8)) id.append(String.format(Locale.ROOT, "%02x", b & 255));
        return id.toString();
    }
    static Profile resolve(String model) {
        if (model != null && !model.isEmpty() && offered.add(model)) seen.add(model);
        Profile p = profiles.get(model); return p == null ? common : p;
    }
    static void load(Configuration c, File dir) {
        common = new Profile(SwayMod.tuning); profiles.clear();
        String[] models = c.get("vehicles", "models", new String[0],
            "車両JSONのモデル名（getModelName）の完全一致。設定画面を開くと検出済み車両も追加されます。削除すると共通設定へ戻ります。").getStringList();
        for (String model : models) {
            if (model.isEmpty() || profiles.containsKey(model)) continue;
            offered.add(model);
            String cat = category(model);
            c.getCategory(cat).setComment("車両: " + model + "。空欄は共通設定を継承。既定値に戻すと全項目を継承します。");
            c.getCategory(cat).setLanguageKey(model);
            Profile p = new Profile(SwayMod.tuning);
            p.isOn = bool(c, cat, "enabled", p.isOn); p.isVertical = bool(c, cat, "vertical", p.isVertical);
            p.gain = number(c, cat, "strength", p.gain, 0, 5);
            p.run = number(c, cat, "running", p.run, 0, 5);
            p.curve = number(c, cat, "curve", p.curve, 0, 5);
            p.branch = number(c, cat, "switch", p.branch, 0, 5);
            p.stop = number(c, cat, "stop", p.stop, 0, 5);
            p.pivot = number(c, cat, "pivotHeight", p.pivot, 0, 4);
            for (Tuning.Spec s : Tuning.SPECS.values()) {
                String v = text(c, cat, s.path, s.comment + "。空欄=共通、範囲=" + s.min + "～" + s.max);
                if (!v.isEmpty()) try { p.tuning.values.put(s.path, s.check(Double.valueOf(v))); }
                catch (IllegalArgumentException e) { warn(model, s.path, e); }
            }
            String notch = text(c, cat, "stop.notchFactors", "空欄=共通。N,B1～B7,非常の9個をカンマ区切りで指定（0～10）");
            if (!notch.isEmpty()) try {
                List<Double> ns = new ArrayList<Double>(); for (String n : notch.split(",", -1)) ns.add(Double.valueOf(n.trim()));
                p.tuning.notch = Tuning.checkNotch(ns);
            } catch (IllegalArgumentException e) { warn(model, "stop.notchFactors", e); }
            String key = text(c, cat, "straight.dataMapKey", "空欄=共通、-でDataMap補正を無効。その他は128文字以内のキー");
            if (!key.isEmpty()) {
                if (key.length() <= 128) p.tuning.dataMapKey = key.equals("-") ? "" : key;
                else LOG.warning(model + ": DataMapキーが長すぎます。共通設定を使用します。");
            }
            String file = text(c, cat, "previewFile", "config/rtmclientsway内のJSファイル名。importPreviewで一度だけこの車両へ取り込み");
            if (c.get(cat, "importPreview", false, "Previewer出力をこの車両だけに取り込む。省略項目は既存の車両別設定を保持").getBoolean(false))
                importFile(c, dir, cat, file, p, model);
            profiles.put(model, p);
        }
    }
    static void prepareGui(Configuration c, File dir) {
        Set<String> models = new LinkedHashSet<String>(Arrays.asList(c.get("vehicles", "models", new String[0]).getStringList()));
        models.addAll(seen);
        seen.clear();
        c.get("vehicles", "models", new String[0]).set(models.toArray(new String[0]));
        load(c, dir); c.save();
    }
    private static void importFile(Configuration c, File dir, String cat, String file, Profile p, String model) {
        try {
            Path root = new File(dir, "rtmclientsway").toPath().toAbsolutePath().normalize();
            Path source = root.resolve(file).normalize();
            if (file.isEmpty() || !source.getParent().equals(root) || !source.toRealPath().getParent().equals(root.toRealPath()))
                throw new IOException("同ディレクトリ内のファイル名を指定してください");
            if (Files.size(source) > 262144) throw new IOException("上限256KiB");
            Map<String, Object> input = TuningParser.parse(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            Tuning next = copy(p.tuning); List<String> ignored = new ArrayList<String>(); next.overlay(input, ignored);
            Path old = new File(dir, "rtmclientsway.cfg").toPath();
            if (Files.exists(old)) Files.copy(old, old.resolveSibling("rtmclientsway.cfg.before-vehicle-import-" + System.nanoTime() + ".bak"));
            for (Tuning.Spec s : Tuning.SPECS.values()) if (contains(input, s.group, s.key)) c.get(cat, s.path, "").set(Double.toString(next.get(s.path)));
            if (contains(input, "stop", "notchFactors")) {
                StringBuilder v = new StringBuilder(); for (double n : next.notch) { if (v.length() > 0) v.append(','); v.append(n); }
                c.get(cat, "stop.notchFactors", "").set(v.toString());
            }
            if (contains(input, "straight", "dataMapKey")) c.get(cat, "straight.dataMapKey", "").set(next.dataMapKey.isEmpty() ? "-" : next.dataMapKey);
            p.tuning = next; c.get(cat, "importPreview", false).set(false);
            LOG.info(model + ": 車両別Previewer設定を取り込み。未適用=" + ignored);
        } catch (IOException | IllegalArgumentException e) { warn(model, "Previewer取り込み", e); }
    }
    private static boolean contains(Map<String, Object> map, String group, String key) {
        Object g = map.get(group); return g instanceof Map && ((Map<?, ?>)g).containsKey(key);
    }
    private static String text(Configuration c, String cat, String key, String comment) { return c.get(cat, key, "", comment).getString().trim(); }
    private static boolean bool(Configuration c, String cat, String key, boolean fallback) {
        String s = text(c, cat, key, "空欄=共通、true=ON、false=OFF");
        if (s.isEmpty()) return fallback;
        if (s.equalsIgnoreCase("true")) return true; if (s.equalsIgnoreCase("false")) return false;
        LOG.warning(cat + ": " + key + " はtrue/false。共通設定を使用します。"); return fallback;
    }
    private static double number(Configuration c, String cat, String key, double fallback, double min, double max) {
        String s = text(c, cat, key, "空欄=共通、範囲=" + min + "～" + max);
        if (!s.isEmpty()) try {
            double n = Double.parseDouble(s); if (!Double.isFinite(n) || n < min || n > max) throw new IllegalArgumentException("範囲外"); return n;
        } catch (IllegalArgumentException e) { warn(cat, key, e); }
        return fallback;
    }
    private static void warn(String model, String key, Exception e) { LOG.warning(model + ": " + key + " を適用しません: " + e.getMessage()); }
}
