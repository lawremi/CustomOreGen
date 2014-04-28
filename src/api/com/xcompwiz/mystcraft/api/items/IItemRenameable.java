package com.xcompwiz.mystcraft.api.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * This interface allows you to create your own items which the Mystcraft writing desk can rename.
 * Have your items implement it to allow them to be renameable in the desk.
 * @author xcompwiz
 */
public interface IItemRenameable {

	/**
	 * Retrieves the display name for the item.
	 * This is the text that appears in the text box.
	 * Note that this is typically called client-side.
	 * @param player The player who is viewing the name
	 * @param itemstack The itemstack instance of the item
	 * @return The name to display for the item
	 */
	String getDisplayName(EntityPlayer player, ItemStack itemstack);

	/**
	 * Called to change the name of the item.
	 * You may safely ignore this, if you wish.
	 * Note that this is primarily called server-side.
	 * @param player The player changing the name
	 * @param itemstack The itemstack instance of the item
	 * @param name The name to change to
	 */
	void setDisplayName(EntityPlayer player, ItemStack itemstack, String name);
}
