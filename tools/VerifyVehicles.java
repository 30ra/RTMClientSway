package rtmsway;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.minecraftforge.common.config.Configuration;

public final class VerifyVehicles {
    private static void check(boolean isOk, String msg) { if (!isOk) throw new AssertionError(msg); }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory(Paths.get("build/verify"), "vehicles-");
        java.lang.reflect.Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null, dir.toFile());
        File file = dir.resolve("rtmclientsway.cfg").toFile();
        Configuration c = new Configuration(file); c.load();
        SwayMod.tuning = new Tuning(); SwayMod.tuning.values.put("curve.leanRollDeg", 1.7);
        SwayMod.isOn = false; SwayMod.gain = 1;
        c.get("vehicles", "models", new String[0]).set(new String[]{"八高.8000", "B"});
        String a = VehicleConfig.category("八高.8000"), b = VehicleConfig.category("B");
        c.get(a, "enabled", "").set("true"); c.get(a, "curve.leanRollDeg", "").set("2.3");
        VehicleConfig.load(c, dir.toFile()); c.save();
        check(VehicleConfig.resolve("八高.8000").isOn, "per-model ON overrides common OFF");
        check(!VehicleConfig.resolve("B").isOn && !VehicleConfig.resolve("unknown").isOn, "common fallback");
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.leanRollDeg") == 2.3, "exact override");
        check(VehicleConfig.resolve("B").tuning.get("curve.leanRollDeg") == 1.7, "separate profile");
        check(!a.equals(VehicleConfig.category("八高_8000")), "model category cannot collide");
        SwayMod.tuning.values.put("curve.leanRollDeg", 1.9); VehicleConfig.load(c, dir.toFile());
        check(VehicleConfig.resolve("B").tuning.get("curve.leanRollDeg") == 1.9, "inherit common edits");
        c.get(a, "curve.leanRollDeg", "").set(""); c.get(a, "curve.rollFrequencyHz", "").set("NaN");
        VehicleConfig.load(c, dir.toFile());
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.leanRollDeg") == 1.9, "empty reset inherits");
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.rollFrequencyHz") == .75, "invalid override falls back");
        c.get(a, "curve.rollFrequencyHz", "").set("");
        Path source = dir.resolve("rtmclientsway/a.js"); Files.createDirectories(source.getParent());
        Files.write(source, "var MOTION_TUNING={curve:{leanRollDeg:2.5},stop:{notchFactors:[0,0,0,0,0,1,2,3,4]}};".getBytes(StandardCharsets.UTF_8));
        c.get(a, "previewFile", "").set("a.js"); c.get(a, "importPreview", false).set(true);
        VehicleConfig.load(c, dir.toFile()); c.save();
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.leanRollDeg") == 2.5, "model import");
        check(VehicleConfig.resolve("B").tuning.get("curve.leanRollDeg") == 1.9, "import isolated");
        check(!c.get(a, "importPreview", false).getBoolean(false), "one-shot import");
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            check(files.anyMatch(p -> p.toString().contains("before-vehicle-import")), "import backup");
        }
        Configuration reopened = new Configuration(file); reopened.load(); VehicleConfig.load(reopened, dir.toFile());
        check(VehicleConfig.resolve("八高.8000").tuning.notch[8] == 4, "saved array");
        Files.write(source, "var MOTION_TUNING={curve:{leanRollDeg:3,rollFrequencyHz:0}};".getBytes(StandardCharsets.UTF_8));
        reopened.get(a, "importPreview", false).set(true); VehicleConfig.load(reopened, dir.toFile());
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.leanRollDeg") == 2.5, "atomic invalid import");
        reopened.get(a, "previewFile", "").set("../a.js"); VehicleConfig.load(reopened, dir.toFile());
        check(VehicleConfig.resolve("八高.8000").tuning.get("curve.leanRollDeg") == 2.5, "path escape rejected");
        reopened.get(a, "importPreview", false).set(false);
        VehicleConfig.seen.clear(); VehicleConfig.resolve("new.train"); VehicleConfig.prepareGui(reopened, dir.toFile());
        check(VehicleConfig.profiles.containsKey("new.train"), "discovered model available in GUI");
        reopened.get("vehicles", "models", new String[0]).set(new String[]{"B"}); VehicleConfig.load(reopened, dir.toFile());
        check(VehicleConfig.resolve("八高.8000") == VehicleConfig.common, "removed profile ignored");
        VehicleConfig.prepareGui(reopened, dir.toFile());
        check(!VehicleConfig.profiles.containsKey("八高.8000"), "removed profile not silently re-added during session");
        System.out.println("PASS: model isolation, inheritance, exact names, ON/OFF, reset, import, backup, persistence, atomic failure, path guard, discovery");
    }
}
