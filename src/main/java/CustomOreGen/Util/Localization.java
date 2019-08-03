package CustomOreGen.Util;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
