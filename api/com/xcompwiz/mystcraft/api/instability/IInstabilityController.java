package com.xcompwiz.mystcraft.api.instability;

import com.xcompwiz.mystcraft.api.symbol.logic.IEnvironmentalEffect;

/**
 * Interface to the controller system initialized along with an age that controls what instability mechanics are added to the age
 * An instance of this will be passed to your InstabilityProviders to allow logic registration
 * Do NOT implement this yourself!
 */
public interface IInstabilityController {

	/**
	 * Returns the world's total instability score without the stability provided by the registered mechanics
	 */
	public int getInstabilityScore();

	/**
	 * Call this to register your effects when an InstabilityProvider is instantiated on a world
	 */
	public void registerEffect(IEnvironmentalEffect effect);

}
