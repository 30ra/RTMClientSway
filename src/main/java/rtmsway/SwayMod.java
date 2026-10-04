package rtmsway;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.common.config.Configuration;

@Mod(modid="rtmclientsway", name="RTM Client Sway", version="1.0.0", acceptedMinecraftVersions="[1.7.10]", acceptableRemoteVersions="*", guiFactory="rtmsway.SwayGuiFactory")
public final class SwayMod {
    public static Configuration config;
    public static boolean isOn = true, isVertical = true;
    public static double gain = 1, run = 1, curve = 1, branch = 1, stop = 1, pivot = 1.5;
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        if (!event.getSide().isClient()) return;
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        reload();
        FMLCommonHandler.instance().bus().register(this);
    }
    public static void reload() {
        isOn = config.getBoolean("enabled", "sway", true, "車体の揺れを有効にする");
        isVertical = config.getBoolean("vertical", "sway", true, "走行・停車時の上下動。分岐には上下衝撃を加えません");
        gain = value("strength", 1, 0, 5, "全体の強さ。1が八高8000スクリプト相当");
        run = value("running", 1, 0, 5, "走行中の揺れの強さ");
        curve = value("curve", 1, 0, 5, "カーブの追加傾斜の強さ");
        branch = value("switch", 1, 0, 5, "分岐の傾斜・横衝撃の強さ");
        stop = value("stop", 1, 0, 5, "停車時の揺り返しの強さ");
        pivot = value("pivotHeight", 1.5, 0, 4, "回転中心の高さ(m)");
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
