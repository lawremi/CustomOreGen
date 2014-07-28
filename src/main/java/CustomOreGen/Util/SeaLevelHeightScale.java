package CustomOreGen.Util;

import net.minecraft.world.World;

public class SeaLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		int seaLevel = world.provider.getAverageGroundLevel();
		if (world.provider.isHellWorld)
			seaLevel /= 2;
		return seaLevel;
	}

	@Override
	public String getName() {
		return "sealevel";
	}

}
