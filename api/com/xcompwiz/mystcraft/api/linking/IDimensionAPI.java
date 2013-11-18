package com.xcompwiz.mystcraft.api.linking;

import java.util.Collection;

/**
 * Functions for interacting with Mystcraft dimensions.
 * The implementation of this is provided by MystAPI.
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface IDimensionAPI {

	/**
	 * @return An immutable list of all the dimension ids which are Mystcraft dimensions
	 */
	public Collection<Integer> getAllAges();

	/**
	 * Used to evaluate if a dimension is a Mystcraft age or not
	 * @param dimId The id of the dimension
	 * @return Returns true is the dimension is a Mystcraft Age
	 */
	public boolean isMystcraftAge(int dimId);

	/**
	 * Creates a new dimension as a Mystcraft Age.
	 * Mystcraft will handle tracking of the age for start up and communicate 
	 * the id to the client for you.
	 * Note that this will only work server side.
	 * @return The id of the newly created dimension
	 */
	public int createAge();
}
