package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class PositionHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		return WorldGenQueries.getSurfaceHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "position";
	}

}
