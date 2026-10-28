package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class BaseHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		return 64;
	}

	@Override
	public String getName() {
		return "base";
	}

}
