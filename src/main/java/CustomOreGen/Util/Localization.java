package CustomOreGen.Util;

import net.minecraft.locale.Language;

public class Localization {
	public static String maybeLocalize(String key, String defaultValue) {
		return Language.getInstance().getOrDefault(key, defaultValue);
	}
}
