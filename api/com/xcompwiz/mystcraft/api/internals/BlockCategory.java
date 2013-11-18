package com.xcompwiz.mystcraft.api.internals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Defines the categories for the BlockDescriptors.
 * @author xcompwiz
 */
public final class BlockCategory {

	private static HashMap<String, BlockCategory> categories = new HashMap<String, BlockCategory>();

	/**
	 * Gets all of the generation categories
	 * @return The set of all generation categories
	 */
	public static Collection<BlockCategory> getCategories() {
		return Collections.unmodifiableCollection(categories.values());
	}

	private final String name;

	/** Valid Terrain blocks may be used in terrain gen */
	public static final BlockCategory TERRAIN   = new BlockCategory("BlockTerrain");
	/** Valid Solid blocks include sand (everything but fluids and air) */
	public static final BlockCategory SOLID     = new BlockCategory("BlockSolid");
	/** Valid Structure blocks are solid and do not fall */
	public static final BlockCategory STRUCTURE = new BlockCategory("BlockStructure");
	/** Valid Crystal blocks may be used in crystalline formations */
	public static final BlockCategory CRYSTAL   = new BlockCategory("BlockCrystal");
	/** Valid Sea blocks may be used in terrain gen as the ocean blocks */
	public static final BlockCategory SEA       = new BlockCategory("BlockSea");
	/** Valid Fluid blocks are fluids (not solid or air) */
	public static final BlockCategory FLUID     = new BlockCategory("BlockFluid");
	/** Valid Fluid blocks are fluids (not solid or air) */
	public static final BlockCategory GAS       = new BlockCategory("BlockGas");
	/** All block modifiers satisfy this */
	public static final BlockCategory ANY       = new BlockCategory("BlockAny");

	/**
	 * Creates a new BlockCategory.
	 * Generally you shouldn't make more than one instance with a name, but it is supported for the sake of multiple add-ons wanting identical new block types
	 * @param name The name of the category.  Used for comparisons and as the category's grammar token
	 */
	public BlockCategory(String name) {
		this.name = name;
		categories.put(name, this);
	}

	/**
	 * Returns the type name of the BlockCategory
	 * @return the name provided to the BlockCategory
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the grammar token bound to a generation category
	 * @return The grammar token for the category
	 */
	public String getGrammarBinding() {
		return this.name;
	}
}
