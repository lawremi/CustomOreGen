package CustomOreGen.Util;

import CustomOreGen.Integration.ATG.ATGIntegration;
import cpw.mods.fml.common.Loader;
import net.minecraft.world.World;

public class PositionHeightScale implements HeightScale {

	@Override
	public int getHeight(World world, int x, int z) {
		if (Loader.isModLoaded("ATG")) {
			if (ATGIntegration.worldIsATG(world)) {
				return ATGIntegration.getSurfaceHeight(world, x, z);
			}
		}
		return new BiomeHeightScale().getHeight(world, x, z);
	}

	@Override
	public String getName() {
		return "position";
	}

}
