package com.xcompwiz.mystcraft.api.items;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.xcompwiz.mystcraft.api.internals.PositionableItem;

/**
 * This interface allows you to create your own items which the Mystcraft writing desk can use as symbol sources (the tabs on the left).
 * Have your items implement it to allow the desk to get symbols from them for writing.
 * @author xcompwiz
 */
public interface IItemSymbolProvider extends IItemRenameable {

	/**
	 * Called when the player tries to remove a page from the itemstack.
	 * If you do not wish this to be possible, simply return null.
	 * @param player The player getting the page
	 * @param itemstack The itemstack instance of the item the page is being removed from
	 * @param index The slot index of the page.  This is the slot id provided by your PositionableItem instances
	 * @return The page retrieved as an itemstack
	 */
	ItemStack removePage(EntityPlayer player, ItemStack itemstack, int index);

	/**
	 * Called to retrieve the list of pages and their locations in the itemstack.
	 * This is used by the surface to arrange the pages spatially.
	 * This can safely return null
	 * @param player The current player
	 * @param itemstack The itemstack instance of this item
	 * @return A list of pages in the item as {@link PositionableItem}s
	 */
	List<PositionableItem> getSymbolListForSurface(EntityPlayer player, ItemStack itemstack);

	/**
	 * Called when the player tries to add a page to the itemstack.
	 * This method is typically done by the "on tab" drop, rather than to the surface.
	 * If you do not wish to accept the page, simply return the page.
	 * @param player The player adding the page
	 * @param itemstack The itemstack instance of this item
	 * @param page The page being added. Not guaranteed to be a page item.  May have stacksize > 1.
	 * @return The reject of the operation. The player's cursor item will be set to this.
	 */
	ItemStack addPage(EntityPlayer player, ItemStack itemstack, ItemStack page);

	/**
	 * Called when the player tries to add a page to the itemstack.
	 * This method is typically called when the player clicks somewhere on the surface with an item in hand.
	 * If you do not wish to accept the page, simply return the page.
	 * @param player The player adding the page
	 * @param itemstack The itemstack instance of this item
	 * @param page The page being added. Not guaranteed to be a page item.  May have stacksize > 1.
	 * @param x The x coordinate clicked
	 * @param y The y coordinate clicked
	 * @return The reject of the operation. The player's cursor item will be set to this.
	 */
	ItemStack addPage(ItemStack itemstack, ItemStack page, float x, float y);

	/**
	 * Called when the provider should sort the pages.
	 * Only the positions need to be changed, the slot numbers may remain the same.
	 * This can be ignored, if you do not wish to sort at all.
	 * @param itemstack The itemstack instance of this item
	 * @param type The type of sort to perform
	 * @param width The width of the surface area you are fitting your items into
	 */
	void sort(ItemStack itemstack, SortType type, short width);

	public static enum SortType {
		ALPHABETICAL,
	}
}
