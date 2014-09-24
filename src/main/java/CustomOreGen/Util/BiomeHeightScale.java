package CustomOreGen.Util;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderGenerate;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if (world.provider.hasNoSky || biome == null) {
			return new WorldHeightScale().getHeight(world, x, z);
		}
		return this.biomeToBlockHeight(biome.rootHeight, world);
	}

	private int biomeToBlockHeight(float biomeHeight, World world) {
		int groundHeight = world.provider.getAverageGroundLevel();
		if (world.provider.terrainType == WorldType.AMPLIFIED && biomeHeight > 0) {
			biomeHeight = 1.0F + biomeHeight * 2.0F; 
		}
		return (int)(groundHeight + Math.min(biomeHeight, 4.0) / 2 * groundHeight);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
