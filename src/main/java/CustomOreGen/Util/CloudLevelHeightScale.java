package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public class CloudLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		return (int)world.getLevelInfo().getGenerator().getCloudHeight();
	}

	@Override
	public String getName() {
		return "cloudlevel";
	}

}
