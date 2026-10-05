package rtmsway;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.7.10")
// 起動・変換処理だけを除外し、SwayHookはAngelicaのGL変換を受けられるようにする。
@IFMLLoadingPlugin.TransformerExclusions({"rtmsway.CorePlugin", "rtmsway.SwayTransformer"})
public final class CorePlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[]{"rtmsway.SwayTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {}
    public String getAccessTransformerClass() { return null; }
}
