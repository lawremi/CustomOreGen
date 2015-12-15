package CustomOreGen.Util;

import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.WorldConfig;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if (biome == null) {
			return new WorldHeightScale().getHeight(world, x, z);
		}
		return this.calcBlockHeight(world, biome.rootHeight, biome.heightVariation);
	}

	private int calcBlockHeight(World world, float rootHeight, float heightVariation) {
		WorldConfig config = ServerState.getWorldConfig(world);
		if (world.provider.terrainType == WorldType.AMPLIFIED && rootHeight > 0) {
			rootHeight = 1.0F + rootHeight * 2.0F; 
		}
		return (int)(
				world.provider.getAverageGroundLevel() +
				rootHeight * config.rootHeightFactor + 
				heightVariation * config.heightVarFactor +
				rootHeight * heightVariation * config.rootHeightVarFactor);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
