package com.xcompwiz.mystcraft.api.instability;

/**
 * Provides an instability mechanic description and registers effects using the same approach as the symbols
 * Implement and register this through the InstabilityAPI to add your own instability mechanics to Mystcraft
 */
public interface IInstabilityProvider {

	/**
	 * This needs to provide a unique string identifier for this mechanic.
	 * @return A unique id
	 */
	public String identifier();

	/**
	 * Provides the amount of stability the mechanic provides.
	 * @return The amount of stability to provide every time this mechanic is added
	 */
	public int stabilization();
	/**
	 * Returns the low end of the interval in which this instability mechanic can be added.
	 * The mechanic will only be added to a world if the world's instability score is greater than or equal to this value.
	 * Can return null (negative infinity).
	 * @return The low end of the required instability range
	 */
	public Integer minimumInstability();
	/**
	 * Returns the high end of the interval in which this instability mechanic can be added.
	 * The mechanic will only be added to a world if the world's instability score is less than or equal to this value.
	 * Can return null (positive infinity).
	 * @return The high end of the required instability range
	 */
	public Integer maximumInstability();

	/**
	 * Called when the provider should register its effects to the passed controller.
	 * This will be called only once during age construction, no matter what level the provider is for the world (regardless of the number of times 'stacked').
	 * @param level The level (stack count) of the provider [1-inf)
	 */
	public void addEffects(IInstabilityController controller, Integer level);

	/**
	 * Should return the maximum level of the mechanic, ie. the number of times the provider can stack.
	 * Can return null to indicate no maximum.
	 * @return the maximum level the provider can handle
	 */
	public Integer maxLevel();
}
