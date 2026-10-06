package rtmsway;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.common.config.Configuration;
import java.io.File;

@Mod(modid="rtmclientsway", name="RTM Client Sway", version="1.1.0-dev", acceptedMinecraftVersions="[1.7.10]", acceptableRemoteVersions="*", guiFactory="rtmsway.SwayGuiFactory")
public final class SwayMod {
    public static Configuration config;
    static Tuning tuning = new Tuning();
    private static File configDir;
    public static boolean isOn = true, isVertical = true;
    public static double gain = 1, run = 1, curve = 1, branch = 1, stop = 1, pivot = 1.5;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        config = new Configuration(event.getSuggestedConfigurationFile());
        configDir = event.getModConfigurationDirectory();
        config.load();
        reload();
        FMLCommonHandler.instance().bus().register(this);
    }
    public static void reload() {
        isOn = config.getBoolean("enabled", "sway", true, "車体の揺れを有効にする");
        isVertical = config.getBoolean("vertical", "sway", true, "走行・分岐時の上下動");
        gain = value("strength", 1, 0, 5, "すべての揺れに掛ける倍率。1が標準、0で揺れなし");
        run = value("running", 1, 0, 5, "走行中の揺れの強さ");
        curve = value("curve", 1, 0, 5, "カーブの追加傾斜の強さ");
        branch = value("switch", 1, 0, 5, "分岐の傾斜・横衝撃の強さ");
        stop = value("stop", 1, 0, 5, "停車時の揺り返しの強さ");
        pivot = value("pivotHeight", 1.5, 0, 4, "回転中心の高さ(m)");
        tuning = TuningConfig.load(config, configDir);
        config.getCategory("sway").setLanguageKey("rtmclientsway.config.general");
        for (String name : new String[]{"curve", "straight", "turnout", "stop"})
            config.getCategory(name).setLanguageKey("rtmclientsway.config." + name);
        config.save();
        SwayHook.clear();
    }
    private static double value(String key, double def, double min, double max, String comment) {
        return config.getFloat(key, "sway", (float)def, (float)min, (float)max, comment);
    }
    @SubscribeEvent public void changed(ConfigChangedEvent.OnConfigChangedEvent event) {
        if ("rtmclientsway".equals(event.modID)) reload();
    }
}
