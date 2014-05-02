package CustomOreGen.Integration.ATG;

import net.minecraft.world.World;

public class ATGIntegration {

	public static boolean worldIsATG(World world) {
		return false; //ATGAPI.WorldIsATG(world);
	}

	public static int getSurfaceHeight(World world, int x, int z) {
		double normalizedHeight = 0; //ATGBiomes.getGeneratorInfo(x, z).get(0);
		return (int)(normalizedHeight * world.getHeight());
	}

}
