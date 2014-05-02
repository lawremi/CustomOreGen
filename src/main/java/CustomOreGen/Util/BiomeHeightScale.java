package CustomOreGen.Util;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		if (world.provider.hasNoSky || biome == null) {
			return new WorldHeightScale().getHeight(world, x, z);
		}
		return (int)biome.rootHeight;
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
