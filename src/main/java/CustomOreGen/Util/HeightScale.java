package CustomOreGen.Util;

import net.minecraft.world.level.LevelAccessor;

public interface HeightScale {
	public int getHeight(LevelAccessor world, int x, int z);
	public String getName();
}
