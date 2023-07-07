package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class SeaLevelHeightScale implements HeightScale {

	@SuppressWarnings("deprecation")
	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		return world.getSeaLevel();	
	}

	@Override
	public String getName() {
		return "sealevel";
	}

}
