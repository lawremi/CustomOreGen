package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Adds a sky drop to the age
 * This is rendered in front of the stars but behind the sun and the moon
 * @author XCompWiz
 *
 */
//FIXME: Stable?
public interface ISkyDoodad {

	/**
	 * The celestial body's render pass
	 */
	@SideOnly(Side.CLIENT)
	public abstract void render(TextureManager textureManager, World worldObj, float partialTicks);
}
