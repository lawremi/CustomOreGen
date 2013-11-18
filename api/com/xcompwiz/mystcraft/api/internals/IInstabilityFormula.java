package com.xcompwiz.mystcraft.api.internals;

/**
 * A basic algebra math formula interface for calculating the instability to produce when using a block descriptor. 
 * @author xcompwiz
 */
public interface IInstabilityFormula {

	/**
	 * Returns a value based on the value of x
	 * @param clustersPerChunk The average number of clusters per chunk. 0 implies a single cluster in the world.
	 * @param blocksPerCluster The average number of blocks the generator will add per cluster
	 * @return The y position
	 */
	public abstract float calc(float clustersPerChunk, float blocksPerCluster);
}
