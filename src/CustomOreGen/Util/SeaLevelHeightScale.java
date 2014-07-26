package CustomOreGen.Util;

import net.minecraft.world.World;

public class SeaLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		return world.provider.getAverageGroundLevel();
	}

	@Override
	public String getName() {
		return "sealevel";
	}

}
