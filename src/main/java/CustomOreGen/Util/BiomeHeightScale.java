package CustomOreGen.Util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.level.level.LevelType;
import net.minecraft.level.level.biome.Biome;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		Biome biome = world.getBiome(new BlockPos(x, 0, z));
		if (biome == null) {
			return new LevelHeightScale().getHeight(world, x, z);
		}
		return this.calcBlockHeight(world, biome.getDepth());
	}

	private int calcBlockHeight(Level world, float rootHeight) {
		if (world.getLevelType() == LevelType.AMPLIFIED && rootHeight > 0) {
			rootHeight = 1.0F + rootHeight * 2.0F; 
		}
		return (int)(world.getHorizon() + rootHeight * 17);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
