package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public class PositionHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		return new BiomeHeightScale().getHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "position";
	}

}
