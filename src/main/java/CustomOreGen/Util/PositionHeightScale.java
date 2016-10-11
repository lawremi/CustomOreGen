package CustomOreGen.Util;

import net.minecraft.world.World;

public class PositionHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		return new BiomeHeightScale().getHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "position";
	}

}
