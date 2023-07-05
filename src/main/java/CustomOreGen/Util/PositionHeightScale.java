package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public class PositionHeightScale implements HeightScale {

	@Override
	public int getHeight(LevelAccessor world, int x, int z) {
		return new BiomeHeightScale().getHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "position";
	}

}
