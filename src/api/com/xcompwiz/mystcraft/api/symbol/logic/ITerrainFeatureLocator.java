package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;

/**
 * This is used for locating things like Strongholds 
 * @author xcompwiz
 */
public interface ITerrainFeatureLocator {

	/**
	 * Returns the location of the nearest instance of the named location.
	 * Can return null if there aren't any instances or if the named location is unrecognized
	 * You only need one of these if you are adding a structure you want to be able to locate using an item, akin to how Strongholds may be found
	 * @param world The world object of the dimension
	 * @param identifier The name of the location or element to locate
	 * @param x Block coordinates
	 * @param y Block coordinates
	 * @param z Block coordinates
	 * @return
	 */
	public abstract ChunkPosition locate(World world, String identifier, int x, int y, int z);
}