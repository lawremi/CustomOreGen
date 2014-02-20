package CustomOreGen.Util;

import net.minecraft.world.World;

public interface HeightScale {
	public int getHeight(World world, int x, int z);
	public String getName();
}
