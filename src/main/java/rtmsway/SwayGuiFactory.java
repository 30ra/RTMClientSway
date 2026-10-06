package rtmsway;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import cpw.mods.fml.client.config.IConfigElement;

public final class SwayGuiFactory implements IModGuiFactory {
    public void initialize(Minecraft minecraft) {}
    public Class<? extends GuiScreen> mainConfigGuiClass() { return Settings.class; }
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() { return null; }
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) { return null; }
    public static final class Settings extends GuiConfig {
        public Settings(GuiScreen parent) {
            super(parent, elements(), "rtmclientsway", false, false, "RTM Client Sway / 車体揺れ設定");
        }
        private static List<IConfigElement> elements() {
            VehicleConfig.prepareGui(SwayMod.config, SwayMod.configDir);
            List<IConfigElement> elements = new ArrayList<IConfigElement>();
            for (String name : new String[]{"sway", "curve", "straight", "turnout", "stop", "vehicles"})
                elements.add(new ConfigElement(SwayMod.config.getCategory(name)));
            return elements;
        }
    }
}
