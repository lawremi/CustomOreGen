package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public class BaseHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		return 64;
	}

	@Override
	public String getName() {
		return "base";
	}

}
