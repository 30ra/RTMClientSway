package rtmsway;
import java.io.File;
import java.nio.file.*;
import net.minecraftforge.common.config.Configuration;
/** Forge設定の移行・保存・効果別スイッチを検証する。 */
public final class VerifyTuningConfig {
    private static void check(boolean isOk,String message){if(!isOk)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory(Paths.get("build/verify"),"config-");
        java.lang.reflect.Field home=cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);home.set(null,dir.toFile());
        File file=dir.resolve("rtmclientsway.cfg").toFile();
        Configuration c=new Configuration(file);c.load();
        c.get("sway","importPreview",false).set(true);
        c.get("curve","leanRollDeg",1.2).set(1.8);
        for(String group:new String[]{"curve","straight","turnout","stop"})c.get(group,"enabled",true).set(false);
        c.save();SwayMod.config=c;SwayMod.configDir=dir.toFile();SwayMod.reload();
        check(!c.getCategory("sway").containsKey("importPreview"),"import option removed");
        try(java.util.stream.Stream<Path> files=Files.list(dir)){
            check(files.anyMatch(p->p.toString().contains("before-reference")),"migration backup");
        }
        Configuration reopened=new Configuration(file);reopened.load();
        Tuning t=TuningConfig.load(reopened,dir.toFile());
        check(t.get("curve.leanRollDeg")==1.8,"custom value preserved");
        check(!t.isCurve&&!t.isStraight&&!t.isTurnout&&!t.isStop,"switch persistence");
        BodyMotion m=new BodyMotion(1,20,t);m.impact(true,1,20);
        for(int i=0;i<200;i++)m.update(.05,Math.max(0,20-i*.2),Math.max(0,20-(i-1)*.2),1,0,1,8,false);
        check(m.roll.x==0&&m.sway.x==0&&m.bounce.x==0&&m.branchBounce.x==0&&m.pitch.x==0&&m.shift.x==0&&m.brakeShift.x==0,"disabled effects at rest");
        System.out.println("PASS: config migration, backup, persistence and effect switches");
    }
}
