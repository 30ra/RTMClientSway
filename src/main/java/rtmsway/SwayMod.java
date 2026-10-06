package rtmsway;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.common.config.Configuration;
import java.io.File;

@Mod(modid="rtmclientsway", name="RTM Client Sway", version="1.1.0-dev.7", acceptedMinecraftVersions="[1.7.10]", acceptableRemoteVersions="*", guiFactory="rtmsway.SwayGuiFactory")
public final class SwayMod {
    public static Configuration config;
    static Tuning tuning = new Tuning();
    static File configDir;
    public static boolean isOn = true;
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
        // 設定移行前のcfgを保存する。
        String[] old = {"strength", "running", "curve", "switch", "stop", "vertical", "pivotHeight", "importPreview"};
        boolean isOld = false;
        for (String key : old) isOld |= config.getCategory("sway").containsKey(key);
        if (isOld) {
            try {
                java.nio.file.Path file = new File(configDir, "rtmclientsway.cfg").toPath();
                if (java.nio.file.Files.exists(file)) java.nio.file.Files.copy(file, file.resolveSibling("rtmclientsway.cfg.before-reference-" + System.nanoTime() + ".bak"));
                for (String key : old) config.getCategory("sway").remove(key);
            } catch (java.io.IOException e) {
                java.util.logging.Logger.getLogger("RTMClientSway").warning("旧設定のバックアップに失敗。旧項目は残しますが適用しません: " + e);
            }
        }
        tuning = TuningConfig.load(config, configDir);
        config.getCategory("sway").setLanguageKey("rtmclientsway.config.general");
        for (String name : new String[]{"curve", "straight", "turnout", "stop"})
            config.getCategory(name).setLanguageKey("rtmclientsway.config." + name);
        config.save();
        SwayHook.clear();
    }
    @SubscribeEvent public void changed(ConfigChangedEvent.OnConfigChangedEvent event) {
        if ("rtmclientsway".equals(event.modID)) reload();
    }
}
