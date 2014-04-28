package CustomOreGen.Util;

import net.minecraft.world.World;

public class CloudLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		return (int)world.provider.getCloudHeight();
	}

	@Override
	public String getName() {
		return "cloudlevel";
	}

}
