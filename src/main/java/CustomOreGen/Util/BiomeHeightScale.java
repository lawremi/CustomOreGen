package CustomOreGen.Util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		Biome biome = world.getBiome(new BlockPos(x, 0, z));
		if (biome == null) {
			return new WorldHeightScale().getHeight(world, x, z);
		}
		return this.calcBlockHeight(world, biome.getDepth());
	}

	private int calcBlockHeight(World world, float rootHeight) {
		if (world.getWorldType() == WorldType.AMPLIFIED && rootHeight > 0) {
			rootHeight = 1.0F + rootHeight * 2.0F; 
		}
		return (int)(world.getHorizon() + rootHeight * 17);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
