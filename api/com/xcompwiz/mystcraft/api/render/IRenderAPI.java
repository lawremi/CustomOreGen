package com.xcompwiz.mystcraft.api.render;

import com.xcompwiz.mystcraft.api.internals.Color;
import com.xcompwiz.mystcraft.api.linking.ILinkPanelEffect;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Exposes functions for rendering Mystcraft elements, such as symbols, words, and colors
 * @author xcompwiz
 */
@SideOnly(Side.CLIENT)
public interface IRenderAPI {

	/**
	 * Use this to register your own rendering effects on the linkpanel
	 * @param The renderer to register 
	 */
	void registerRenderEffect(ILinkPanelEffect renderer);

	/**
	 * Can be used to draw a symbol somewhere
	 * @param x The x location in pixels
	 * @param y The y location in pixels
	 * @param zLevel The zLevel we're drawing on
	 * @param scale The size in pixels of the square
	 * @param symbol The symbol to draw
	 */
	public void drawSymbol(float x, float y, float zLevel, float scale, IAgeSymbol symbol);

	/**
	 * Draws a Narayan word from the word list.  If the word does not exist, it will be generated based on the string.
	 * Note that you can also draw D'ni numbers by using for example "1" as the word.  This works for 0-26.
	 * @param x The x location in pixels
	 * @param y The y location in pixels
	 * @param zLevel The zLevel we're drawing on
	 * @param scale The size in pixels of the square
	 * @param word The word to draw
	 */
	public void drawWord(float x, float y, float zLevel, float scale, String word);

	/**
	 * Draws a D'ni color "eye".  The eye will be in and represent the provided color.
	 * @param x The x location in pixels
	 * @param y The y location in pixels
	 * @param zLevel The zLevel we're drawing on
	 * @param radius The size in pixels of the square
	 * @param color The color object to use when drawing the eye
	 */
	public void drawColor(float x, float y, float zLevel, float radius, Color color);
}
