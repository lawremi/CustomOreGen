package CustomOreGen.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.NoiseChunk;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		int def = new WorldHeightScale().getHeight(world, x, z);
		Biome biome = world.getBiome(new BlockPos(x, 0, z)).get();
		if (biome == null) {
			return def;
		}
		NoiseChunk noise = world.getChunk(new BlockPos(x, 0, z)).getOrCreateNoiseChunk((a) -> null);
		return noise.preliminarySurfaceLevel(x, z);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
