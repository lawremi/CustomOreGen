package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import com.xcompwiz.mystcraft.api.internals.Color;

/**
 * Used to produce a color once.  This color cannot change over time.
 * @author xcompwiz
 */
//FIXME: Stable?
public interface IStaticColorProvider {
	public final static String WATER		= "Water";
	public final static String GRASS		= "Grass";
	public final static String FOLIAGE		= "Foliage";

	/**
	 * Returns a color based on the provided information
	 * Note that the produced color is fixed, so this type of element should not use gradients
	 * Can return null if the logic element does not provide a color for this case
	 * @param type String identifier of the type of color
	 * @param worldObj The world object
	 * @param biome The current biome.  Can be null
	 * @return A Mystcraft Color object or null
	 */
	public abstract Color getStaticColor(String type, World worldObj, BiomeGenBase biome);
}