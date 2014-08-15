package CustomOreGen.Util;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.util.StatCollector;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Localization {
	public static String maybeLocalize(String key, String defaultValue) {
		String value = defaultValue;
		String localizedValue = I18n.format(key);
    	if (!localizedValue.equals(key)) {
    		value = localizedValue;
    	}
    	return value;
	}
}
