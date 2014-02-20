package CustomOreGen.Util;

import CustomOreGen.Integration.ATG.ATGIntegration;
import ttftcuts.atg.api.ATGAPI;
import cpw.mods.fml.common.Loader;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		if (world.provider.hasNoSky) {
			return world.provider.getAverageGroundLevel();
		}
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		int maxBlockHeight = biomeToBlockHeight(biome.maxHeight, world);
		int minBlockHeight = biomeToBlockHeight(biome.minHeight, world);
		int avgBlockHeight = (maxBlockHeight + minBlockHeight) / 2;
		return avgBlockHeight;
	}

	private int biomeToBlockHeight(float biomeHeight, World world) {
		int range;
		int groundHeight = world.provider.getAverageGroundLevel();
		if (biomeHeight > 0) {
			range = world.getHeight() - groundHeight;
		} else {
			range = groundHeight;
		}
		return (int)(groundHeight + biomeHeight / 4 * range);
	}
	
	@Override
	public String getName() {
		return "biome";
	}
}
