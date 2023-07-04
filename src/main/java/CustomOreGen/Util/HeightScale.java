package CustomOreGen.Util;

import net.minecraft.world.level.Level;

public interface HeightScale {
	public int getHeight(Level world, int x, int z);
	public String getName();
}
