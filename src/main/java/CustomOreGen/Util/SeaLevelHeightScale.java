package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public class SeaLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		return world.getSeaLevel();
	}

	@Override
	public String getName() {
		return "sealevel";
	}

}
