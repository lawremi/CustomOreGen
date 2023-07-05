package CustomOreGen.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		Biome biome = world.getBiome(new BlockPos(x, 0, z)).get();
		if (biome == null) {
			return new WorldHeightScale().getHeight(world, x, z);
		}
		return this.calcBlockHeight(world, biome.getGenerationSettings());
		//worldgen density functions
		//worldgen world_preset
	}

	private int calcBlockHeight(Level world, float rootHeight) {
		/*if (world.getLevelType() == LevelType.AMPLIFIED && rootHeight > 0) {
			rootHeight = 1.0F + rootHeight * 2.0F; 
		}*/
		return (int)(world.getSeaLevel() + rootHeight * 17);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
