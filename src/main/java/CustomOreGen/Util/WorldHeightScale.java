package CustomOreGen.Util;

import net.minecraft.world.World;

public class WorldHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		if (world.provider.hasNoSky) {
			return world.provider.getActualHeight();
		}
		return world.provider.getAverageGroundLevel();
	}

	@Override
	public String getName() {
		return "world";
	}

}
