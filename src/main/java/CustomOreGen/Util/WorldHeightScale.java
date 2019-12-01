package CustomOreGen.Util;

import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class WorldHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		//getAverageGroundLevel called getMinimumSpawnHeight which resolved to return FLAT ? 4 : world.getSeaLevel() + 1
		return (int) world.getSeaLevel()+1;
	}

	@Override
	public String getName() {
		return "world";
	}

}
