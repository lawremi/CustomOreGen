package com.xcompwiz.mystcraft.api.symbol.logic;

import java.util.Random;

import net.minecraft.world.World;

public interface IPopulate {

	/**
	 * This is called to perform generation on the chunk when the chunk first loads
	 * The coordinates supplied are the world coordinates of the chunk (so chunkcoords * 16) as this is what populators use
	 * This is called after biome decoration logic and before effects
	 * @param worldObj The current world
	 * @param rand The random number generator
	 * @param x The x coordinate of the chunk in world-coordinates
	 * @param y The y coordinate of the chunk in world-coordinates
	 * @param flag Originally a vanilla flag meaning "villages generated here." Due to ordering issues of symbols, now rather meaningless.
	 * @return True if flag should change to true. False does not reset flag to false.
	 */
	public abstract boolean populate(World worldObj, Random rand, int x, int y, boolean flag);
}