package CustomOreGen.Util;

import net.minecraft.world.World;

public class BaseHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		return 64;
	}

	@Override
	public String getName() {
		return "base";
	}

}
