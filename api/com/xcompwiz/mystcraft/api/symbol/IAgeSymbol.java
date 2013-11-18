package com.xcompwiz.mystcraft.api.symbol;

import com.xcompwiz.mystcraft.api.symbol.words.WordData;


/**
 * Implement and register this through the ISymbolAPI to add your own symbols to Mystcraft
 */
public interface IAgeSymbol {

	/**
	 * Called when it is time for the Symbol to register its logic elements to the controller
	 * @param controller The controller to register the logic elements to
	 * @param seed A unique seed generated from the world seed and the symbol position
	 * @param A unique seed for the symbol call. The seed is based on the age seed and the order of the symbols,
	 * providing a deterministic way of making the same symbol produce different results within the same age
	 */
	public abstract void registerLogic(IAgeController controller, long seed);
	/**
	 * How much instability should be added to the world.
	 * This is called every time the symbol is added to the world (if it is stacked).
	 * It is not necessary to use this function to add instability for duplicated critical logic.
	 * Used to limit stacking (ex. After three crystal symbols, add 100 instability every time crystal is added).
	 * @param count How many times the symbol has been added thus far (first time = 1)
	 * @return the amount of instability to add
	 */
	public abstract int instabilityModifier(int count);

	/**
	 * Provides a unique string identifier for the symbol
	 * @return a unique identifier
	 */
	public abstract String identifier();
	/**
	 * Returns the user viewable name
	 * @return Name the user sees for the symbol
	 */
	public abstract String displayName();
	/**
	 * Returns a list of words that are used to render the symbol
	 * Should return 4 words to build a Narayan "poem"
	 * See {@link WordData}
	 * @return 4 element array of words to be mapped to drawn symbol parts
	 */
	public abstract String[] getPoem();
}
