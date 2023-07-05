package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class WorldHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		/*if (world.provider.isSkyColored()) {
			return world.provider.getActualHeight();
		}
		return world.provider.getAverageGroundLevel();*/
		//TODO: this is the best I can find for ground level.
		
		//if(world.dimensionType().hasCeiling()) { ? }
		//return (int) world.getHeight();
		
		return world.getSeaLevel();
	}

	@Override
	public String getName() {
		return "world";
	}

}
