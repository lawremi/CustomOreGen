package com.xcompwiz.mystcraft.api.linking;


import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * This interface allows for you to add render layers to the link panel in a book
 * Register it through the {@link IRenderAPI}
 * @author xcompwiz
 */
@SideOnly(Side.CLIENT)
public interface ILinkPanelEffect {

	/**
	 * Called when the rendering should occur
	 * @param left
	 * @param top
	 * @param width
	 * @param height
	 * @param linkInfo The info of the link for the panel being rendered
	 */
	public void render(int left, int top, int width, int height, ILinkInfo linkInfo);

	/** Called when the book gui element first opens */
	public void onOpen();

}
