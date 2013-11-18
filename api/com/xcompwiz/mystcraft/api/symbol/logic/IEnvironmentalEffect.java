package com.xcompwiz.mystcraft.api.symbol.logic;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Implement this to provide an effect through either an IAgeSymbol or an IInstabilityProvider
 * @author xcompwiz
 */
//FIXME: Stable?
public interface IEnvironmentalEffect {

	/**
	 * This allows the effect to perform generation on the chunk when the chunk first loads
	 * The coordinates supplied are the world coordinates of the chunk (so chunkcoords * 16) as this is what populators use
	 * This is called after other populator logic.  Instability effects are handled last.
	 * @param worldObj The current world
	 * @param rand The random number generator
	 * @param x The x coordinate of the chunk in world-coordinates
	 * @param y The y coordinate of the chunk in world-coordinates
	 */
	public void onChunkPopulate(World worldObj, Random rand, int x, int y);

	/**
	 * This is called every tick on every loaded chunk
	 * This means the execution time of the function must be very, very fast.
	 * @param worldObj The current world
	 * @param chunk The current, loaded chunk on which this is being applied
	 */
	public void tick(World worldObj, Chunk chunk);
}
