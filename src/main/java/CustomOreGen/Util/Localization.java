package CustomOreGen.Util;

import cpw.mods.fml.common.registry.LanguageRegistry;

public class Localization {
	public static String maybeLocalize(String key, String defaultValue) {
		String value = defaultValue;
		String localizedValue = LanguageRegistry.instance().getStringLocalization(key);
    	if (localizedValue != "") {
    		value = localizedValue;
    	}
    	return value;
	}
}
