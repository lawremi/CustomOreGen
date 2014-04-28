package com.xcompwiz.mystcraft.api.symbol;

import com.xcompwiz.mystcraft.api.internals.BlockDescriptor;

/**
 * Provides methods for generating boilerplate {@link IAgeSymbol}s
 * These methods do not register the symbol directly.  Use the {@link ISymbolAPI} for that.
 * The implementation of this is provided by MystAPI
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface ISymbolFactory {

	/**
	 * Creates (but does not register!) a new 'Block Modifier' symbol
	 * The resultant symbol, if registered, will generate its own grammar rules
	 * @param block The block descriptor to use
	 * @param thirdword The third DrawableWord reference for the symbol.  Should be something characteristic (ex. Terrain, Ore, Sea)
	 * @param grammarWeight The weighting for the generated grammar rules. See {@link IGrammarAPI}
	 * @return a new, unregistered modifier symbol that pushes a block to the modifier stack
	 */
	public IAgeSymbol createSymbol(BlockDescriptor block, String thirdword, float grammarWeight);
}
