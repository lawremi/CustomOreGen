package com.xcompwiz.mystcraft.api.instability;

import java.util.Collection;

/**
 * Provides methods for registering providers to and interacting with the instability system
 * The implementation of this is provided by MystAPI
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface IInstabilityAPI {

	/**
	 * Registers an instability provider to the instability system
	 * This makes it available for selection when a world is unstable as governed by its internal
	 * stability values
	 */
	public void registerInstability(IInstabilityProvider provider);

	/**
	 * @return An immutable list of all the instability providers registered
	 */
	public Collection<IInstabilityProvider> getAllInstabilityProviders();

	/**
	 * Maps an identifier to an instability provider
	 * @param identifier The indetifier to map
	 * @return The instability provider with that id or null if there isn't one
	 */
	public IInstabilityProvider getInstabilityProvider(String identifier);
}
