package CustomOreGen.Util;

import net.minecraft.world.World;

public class SeaLevelHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		return world.getSeaLevel();
	}

	@Override
	public String getName() {
		return "sealevel";
	}

}
