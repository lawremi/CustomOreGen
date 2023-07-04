package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public class LevelHeightScale implements HeightScale {

	@Override
	public int getHeight(Level world, int x, int z) {
		/*if (world.provider.isSkyColored()) {
			return world.provider.getActualHeight();
		}
		return world.provider.getAverageGroundLevel();*/
		//TODO: this is the best I can find for ground level.
		return (int) world.getLevelInfo().getGenerator().getHorizon(world);
	}

	@Override
	public String getName() {
		return "world";
	}

}
