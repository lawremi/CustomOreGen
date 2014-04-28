package com.xcompwiz.mystcraft.api.internals;

public interface IWeightedItem {
	
    /**
     * The Rarity is used to determine how often the item is chosen (higher = more often; 0 = no chance)
     */
	public float getRarity();
}
