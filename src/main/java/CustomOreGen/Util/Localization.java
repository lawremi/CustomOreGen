package CustomOreGen.Util;

import net.minecraft.locale.Language;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Localization {
	public static String maybeLocalize(String key, String defaultValue) {
		return Language.getInstance().getOrDefault(key, defaultValue);
	}
}
