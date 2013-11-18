package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Adds a sun to the age
 * @author XCompWiz
 *
 */
//FIXME: Stable?
public interface ISun {

	/**
	 * Returns the position in the period of the (light-giving) celestial body, assuming it cycles
	 * 0 is noon, 0.5 is midnight
	 * As the object progresses thought the sky, the number increases.  At midnight it reaches 0.5, after which it continues increasing until it reaches noon, where it loops back to 0.
	 * @return The normalized period position of the object
	 */
	public abstract float getCelestialPeriod(long time, float partialTime);

	/**
	 * Returns the number of ticks until this solar object is at 0.75 (celestial period position)
	 * Can return null if the sun never rises or if the rising time is unknown
	 * @return The number of ticks from now before the solar object will rise
	 */
	public abstract Long getTimeToSunrise(long time);

	/**
	 * The celestial body's render pass
	 */
	@SideOnly(Side.CLIENT)
	public abstract void render(TextureManager textureManager, World worldObj, float partialTicks);
}
