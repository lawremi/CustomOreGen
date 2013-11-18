package com.xcompwiz.mystcraft.api.symbol.logic;

/**
 * Used to produce the base terrain for an age.
 * @author xcompwiz
 */
public interface ITerrainGenerator {

	/**
	 * Generates the base terrain for the age.
	 * @param chunkX The chunk x coordinate in chunk space
	 * @param chunkZ The chunk z coordinate in chunk space
	 * @param blocks The block array being manipulated (y << 8 | z << 4 | x)
	 * @param metadata The metadata values of the blocks in the previous array (y << 8 | z << 4 | x)
	 */
    public abstract void generateTerrain(int chunkX, int chunkZ, short[] blocks, byte[] metadata);
}