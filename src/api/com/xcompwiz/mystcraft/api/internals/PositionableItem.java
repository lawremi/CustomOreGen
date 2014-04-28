package com.xcompwiz.mystcraft.api.internals;

import net.minecraft.item.ItemStack;

/**
 * Used to give an item a position coordinate pair.
 * Used with the page "surface" in the writing desk
 * @author xcompwiz
 */
public final class PositionableItem {
	/** The itemstack */
	public ItemStack itemstack;
	/** A slot id for item identification by the provider of this element */
	public int slotId;
	/** The x coordinate of the item */ 
	public float x = 0;
	/** The y coordinate of the item */
	public float y = 0;

	public PositionableItem(ItemStack itemstack, int slot) {
		this.itemstack = itemstack;
		this.slotId = slot;
	}

	/** Convenience function for setting the coordinates */
	public void setCoords(float x, float y) {
		this.x = x;
		this.y = y;
	}
}
