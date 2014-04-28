package com.xcompwiz.mystcraft.api.symbol;

import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Allows the setting of various treasure rarity and weighting values for symbols
 * See {@link IGrammarAPI} for setting the grammar rule(s) for a symbol
 * These should be set before post-init
 * The implementation of this is provided by MystAPI
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface ISymbolValuesAPI {
	/**
	 * Sets the weight used when generating treasure and selecting pages for trades
	 * @param symbol The symbol to set the weight for
	 * @param weight The weight to use.  1 is common, 0 is impossible to obtain
	 */
	public void setSymbolItemRarity(IAgeSymbol symbol, float weight);

	/**
	 * Can be used to prevent symbols from being tradeable
	 * By default, a symbol can be traded by a villager
	 * @param symbol The symbol to affect
	 * @param flag Whether or not a village can trade the item
	 */
	public void setSymbolIsTradable(IAgeSymbol symbol, boolean flag);

	/**
	 * Can be used to set the item a villager will trade the symbol for
	 * @param symbol The symbol the villager will trade
	 * @param itemstack An instance of the item that the villager wants to trade for
	 */
	public void setSymbolTradeItem(IAgeSymbol symbol, ItemStack itemstack);

	/**
	 * Can be used to set the items a villager will trade the symbol for.
	 * This version allows the setting of multiple trade items.  Both items will be required for the trade.
	 * @param symbol The symbol the villager will trade
	 * @param itemstack An instance of the first item that the villager wants to trade for
	 * @param secondary An instance of the second item that the villager wants to trade for
	 */
	public void setSymbolTradeItems(IAgeSymbol symbol, ItemStack itemstack, ItemStack secondary);

	/**
	 * @param identifier The identifier of the symbol to use
	 * @return The weight used when generating treasure and selecting pages for trades
	 */
	public float getSymbolItemRarity(String identifier);

	/**
	 * Returns if the symbol is procurable through trade
	 * @param identifier The identifier of the symbol to use
	 * @return True is the symbol can be traded for.  False otherwise.
	 */
	public boolean getSymbolIsTradable(String identifier);

	/**
	 * Returns the asking item(s) in a trade
	 * @param identifier The identifier of the symbol to use
	 * @return The item(s) that the villager is willing to trade for as a list (which is never more than two elements).
	 */
	public List<ItemStack> getSymbolTradeItems(String identifier);
}
