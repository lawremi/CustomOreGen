package com.xcompwiz.mystcraft.api;

import com.xcompwiz.mystcraft.api.instability.IInstabilityAPI;
import com.xcompwiz.mystcraft.api.instability.IInstabilityFactory;
import com.xcompwiz.mystcraft.api.items.IItemFactory;
import com.xcompwiz.mystcraft.api.items.IPageAPI;
import com.xcompwiz.mystcraft.api.linking.IDimensionAPI;
import com.xcompwiz.mystcraft.api.linking.ILinkPropertyAPI;
import com.xcompwiz.mystcraft.api.linking.ILinkingAPI;
import com.xcompwiz.mystcraft.api.render.IRenderAPI;
import com.xcompwiz.mystcraft.api.symbol.IGrammarAPI;
import com.xcompwiz.mystcraft.api.symbol.ISymbolAPI;
import com.xcompwiz.mystcraft.api.symbol.ISymbolFactory;
import com.xcompwiz.mystcraft.api.symbol.ISymbolValuesAPI;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Provides interface implementations for Mystcraft functionality for external use
 * These are not guaranteed to be set.  Be sure to check for nulls.
 * These are set during Mystcraft's pre-init phase
 * @author XCompWiz
 */
public final class MystAPI {
	public static IDimensionAPI				dimensions			= null;
	public static ILinkingAPI				linking				= null;
	public static ILinkPropertyAPI			linkProperties		= null;

	public static IInstabilityAPI			instability			= null;
	public static IInstabilityFactory		instabilityFact		= null;

	public static ISymbolAPI				symbol				= null;
	public static ISymbolValuesAPI			symbolValues		= null;
	public static IGrammarAPI				grammar				= null;
	public static ISymbolFactory			symbolFact			= null;

	public static IPageAPI					page				= null;
	public static IItemFactory				itemFact			= null;

	@SideOnly(Side.CLIENT)
	public static IRenderAPI				render;
}
