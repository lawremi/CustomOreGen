package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.world.chunk.Chunk;

/**
 * Allows for cleanup and finalization tasks to happen to the chunk before population occurs
 * This is the last step of the chunk creation
 * Can be used, for example, to alter the biomes in the chunk or to build some form of data structures.
 * @author xcompwiz
 */
public interface IChunkProviderFinalization {

	/**
	 * Called right after the chunk object has been built, but before population happens
	 * @param chunk The chunk object
	 * @param chunkX The chunk x coordinate in chunk space
	 * @param chunkZ The chunk z coordinate in chunk space
	 */
	void finalizeChunk(Chunk chunk, int chunkX, int chunkZ);

}
