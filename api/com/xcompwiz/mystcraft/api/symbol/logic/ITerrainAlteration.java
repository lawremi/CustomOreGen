package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.world.World;

/**
 * Applied to the chunk data after initial generation
 * @author xcompwiz
 */
public interface ITerrainAlteration {

	/**
	 * Called after the initial terrain generation in order to alter the terrain.
	 * This is called after the biome block replacement
	 * @param worldObj The world object of the chunk being modified.  Do NOT access blocks directly through this.
	 * @param chunkX The chunk x coordinate in chunk space
	 * @param chunkZ The chunk z coordinate in chunk space
	 * @param blocks The block array being manipulated (y << 8 | z << 4 | x)
	 * @param metadata The metadata values of the blocks in the previous array (y << 8 | z << 4 | x)
	 */
    public abstract void alterTerrain(World worldObj, int chunkX, int chunkZ, short[] blocks, byte[] metadata);
}
