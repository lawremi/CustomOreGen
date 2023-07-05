package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class CloudLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		//clouds are considered "client side special effects" now
		//cloud height is defined here
		//OverworldEffects.CLOUD_LEVEL
		return 192;
	}

	@Override
	public String getName() {
		return "cloudlevel";
	}
}
