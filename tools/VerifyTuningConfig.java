package rtmsway;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import net.minecraftforge.common.config.Configuration;

/** Forgeの実際の設定クラスを使った保存・一度だけ取り込みのテスト。 */
public final class VerifyTuningConfig {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory(Paths.get("build/verify"),"tuning-config-");
        java.lang.reflect.Field home=cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null,dir.toFile()); // 本番ではForgeが設定する値。テスト用のみ。
        File cfgFile=dir.resolve("rtmclientsway.cfg").toFile();
        Configuration c=new Configuration(cfgFile); c.load();
        // 廃止した車両別設定を残したcfgでも、共通設定だけが使われる。
        c.get("vehicles", "models", new String[0]).set(new String[]{"LegacyTrain"});
        c.get("vehicles.legacy", "enabled", "").set("false");
        c.get("vehicles.legacy", "curve.leanRollDeg", "").set("9");
        TuningConfig.load(c,dir.toFile()); c.save();
        Path source=dir.resolve("rtmclientsway/MOTION_TUNING.js"); Files.createDirectories(source.getParent());
        Files.write(source,"var MOTION_TUNING={curve:{leanRollDeg:2.4,stageHoldTicks:3},stop:{notchFactors:[0,0,0,0,0,1,2,3,4]}};".getBytes(StandardCharsets.UTF_8));
        c.get("sway","importPreview",false).set(true);
        Tuning imported=TuningConfig.load(c,dir.toFile()); c.save();
        check(imported.get("curve.leanRollDeg")==2.4 && imported.notch[8]==4,"import applied");
        check(!c.get("sway","importPreview",false).getBoolean(false),"one-shot reset");
        check(c.get("curve","stageHoldTicks",2).getInt()==3,"integer property preserved");
        try(java.util.stream.Stream<Path> files=Files.list(dir)){
            check(files.anyMatch(p->p.getFileName().toString().contains("before-import")),"backup created");
        }
        c.get("curve","leanRollDeg",1.2).set(1.8); c.save();
        Configuration reopened=new Configuration(cfgFile); reopened.load();
        Tuning after=TuningConfig.load(reopened,dir.toFile());
        check(after.get("curve.leanRollDeg")==1.8 && after.get("curve.stageHoldTicks")==3,"saved GUI-style edits not overwritten");
        check(reopened.get("vehicles.legacy", "curve.leanRollDeg", "").getString().equals("9"),"legacy profiles preserved but not applied");
        Files.write(source,"var MOTION_TUNING={curve:{leanRollDeg:2,rollFrequencyHz:0}};".getBytes(StandardCharsets.UTF_8));
        reopened.get("sway","importPreview",false).set(true);
        Tuning failed=TuningConfig.load(reopened,dir.toFile());
        check(failed.get("curve.leanRollDeg")==1.8 && reopened.get("curve","leanRollDeg",1.2).getDouble()==1.8,"invalid import is atomic");
        System.out.println("PASS: Forge config persistence, integer fields, arrays, one-shot import, backup, atomic failure; fixtures="+dir);
    }
}
