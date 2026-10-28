package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class BiomeHeightScale implements HeightScale {

	// Biomes no longer carry a base height (1.18+); terrain height comes from noise, so the
	// closest equivalent is the generator's preliminary surface height at this column.
	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		return WorldGenQueries.getSurfaceHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "biome";
	}
}
