package CustomOreGen.Util;

import net.minecraft.world.World;

public class WorldHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		/*if (world.provider.isSkyColored()) {
			return world.provider.getActualHeight();
		}
		return world.provider.getAverageGroundLevel();*/
		//TODO: this is the best I can find for ground level.
		return (int) world.getWorldInfo().getGenerator().getHorizon(world);
	}

	@Override
	public String getName() {
		return "world";
	}

}
