package com.xcompwiz.mystcraft.api.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * This interface allows you to create your own items which the Mystcraft writing desk can write to.
 * Have your items implement it to allow them to be writable in the desk.
 * @author xcompwiz
 */
public interface IItemWritable extends IItemRenameable {

	/**
	 * This is called when the player tries to write a symbol to the item.
	 * @param player The player doing the writing
	 * @param itemstack The itemstack instance of the item being written to
	 * @param symbol The id of the symbol being written
	 * @param paper_feeder This is an itemstack of paper.  You may decrease the stack size to "use" paper.  May be null.
	 * @return True on success
	 */
	boolean writeSymbol(EntityPlayer player, ItemStack itemstack, String symbol, ItemStack paper_feeder);

	/**
	 * Called to retrieve the list of pages in the itemstack
	 * This can safely return null
	 * @param player The current player
	 * @param itemstack The itemstack instance of this item
	 * @return A list of pages in the item
	 */
	List<ItemStack> getPageList(EntityPlayer player, ItemStack itemstack);
}
