package CustomOreGen.Integration.ATG;

import net.minecraft.world.World;
import ttftcuts.atg.api.ATGAPI;
import ttftcuts.atg.api.ATGBiomes;

public class ATGIntegration {

	public static boolean worldIsATG(World world) {
		return ATGAPI.WorldIsATG(world);
	}

	public static int getSurfaceHeight(World world, int x, int z) {
		double normalizedHeight = ATGBiomes.getGeneratorInfo(x, z).get(0);
		return (int)(normalizedHeight * world.getHeight());
	}

}
