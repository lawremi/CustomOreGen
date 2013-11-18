package com.xcompwiz.mystcraft.api.internals;

import java.util.HashMap;


/**
 * For defining information for block modifiers
 * This handles the block modifier system.  Use this to get block modifiers for usage as well as to push block modifiers to the age.
 * See {@link BlockCategory}
 * @author xcompwiz
 */
public final class BlockDescriptor {
	public final short id;
	public final byte metadata;
	private final IInstabilityFormula formula;
	private final HashMap<String, Boolean> useable = new HashMap<String, Boolean>();

	/**
	 * Describes a block
	 * @param blockId ID of the block to use in generation
	 * @param metadata Metadata value of the block to use in generation
	 * @param instability_function A math function which returns the amount of instability to add to the world based on average blocks added per chunk.
	 */
	public BlockDescriptor(short blockId, byte metadata, IInstabilityFormula instability_function) {
		this.id = blockId;
		this.metadata = metadata;
		this.formula = instability_function;
	}

	/**
	 * Describes a block
	 * @param blockId ID of the block to use in generation
	 * @param metadata Metadata value of the block to use in generation
	 */
	public BlockDescriptor(short blockId, byte metadata) {
		this(blockId, metadata, null);
	}

	/**
	 * Describes a block
	 * @param blockId ID of the block to use in generation
	 */
	public BlockDescriptor(short blockId) {
		this(blockId, (byte)0);
	}

	/**
	 * Mark the block descriptor as valid for certain categories of generation
	 * example: Stone is valid for Solid, Structure, and Terrain
	 * @param key The category of terrain which is valid.  See the definitions in the {@link BlockDescriptor} class.
	 * @param flag Whether it is valid or not
	 */
	public void setUsable(BlockCategory key, boolean flag) {
		if (key == null) return;
		if (key == BlockCategory.ANY) return;
		this.useable.put(key.getName(), flag);
	}

	/**
	 * Returns if the block descriptor is flagged for satisfying a category of generation
	 * Generally, you won't need this function.  It is mostly for internal use.
	 * @param key The category to check, null defaults to ANY
	 * @return True is valid, false otherwise
	 */
	public boolean isUsable(BlockCategory key) {
		if (key == null) return true;
		if (key == BlockCategory.ANY) return true;
		if (!this.useable.containsKey(key.getName())) return false;
		return this.useable.get(key.getName());
	}

	/**
	 * Gets the instability value to add to the age based on the average blocks added per chunk.
	 * This uses the provided instability function.
	 * Standard usage is to add instability when registering symbol logic.
	 * Generators should use this and add the result to the age as instability during the controller registration.
	 * @param clustersPerChunk The average number of clusters per chunk
	 * @param blocksPerCluster The average number of blocks the generator will add per cluster
	 * @return The amount of instability to add to the age
	 */
	public int getInstability(float clustersPerChunk, float blocksPerCluster) {
		if (clustersPerChunk < 0) throw new RuntimeException("Cannot generate negative number of clusters!");
		if (blocksPerCluster < 0) throw new RuntimeException("Cannot generate negative blocks per cluster!");
		if (formula == null) return 0;
		return (int)formula.calc(clustersPerChunk, blocksPerCluster);
	}
}
