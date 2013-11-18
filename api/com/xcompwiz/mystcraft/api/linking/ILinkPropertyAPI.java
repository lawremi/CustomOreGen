package com.xcompwiz.mystcraft.api.linking;

import java.util.Collection;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.xcompwiz.mystcraft.api.internals.Color;
import com.xcompwiz.mystcraft.api.internals.ColorGradient;

/**
 * Provides functions for adding link properties
 * Also handles interactions with the ink mixing mechanics
 * The implementation of this is provided by MystAPI
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface ILinkPropertyAPI {

	/* String ids for standard link flags */
	/** The link can stay within the same dimension */
	public static final String FLAG_INTRA_LINKING		= "Intra Linking";
	/** The link will retain offset from main spawn */
	public static final String FLAG_RELATIVE			= "Relative";
	/** The link will cause the player to drop all of their items before linking; items won't link */
	public static final String FLAG_DISARM				= "Disarm";
	/** The link will not nullify the entity's momentum */
	public static final String FLAG_MAINTAIN_MOMENTUM	= "Maintain Momentum";
	/** The link will generate a single block platform on arrival if there is a drop */
	public static final String FLAG_GENERATE_PLATFORM	= "Generate Platform";
	/** The link should be treated as something natural  */
	public static final String FLAG_NATURAL				= "Natural";
	/** The link was initiated by an outside element (not user interaction) */
	public static final String FLAG_EXTERNAL			= "External";
	/** The link was initiated as an attack */
	public static final String FLAG_OFFENSIVE			= "Offensive";

	/** The link was started by a command */
	public static final String FLAG_TPCOMMAND			= "Op-TP";
	/** This is used for books themselves */
	public static final String FLAG_FOLLOWING			= "Following";

	/* String ids for standard link properties */
	/**
	 * Used to indicate what sound should be played when the link occurs
	 * Ignored if the link has the disarm flag or if the linked object is an item
	 * The value of the property should be the reference of the sound to play, for example: "mystcraft:linking.link" 
	 */
	public static final String PROP_SOUND				= "Sound";

	/**
	 * Registers a link property name and sets the color which represents it.
	 * You can use linking events to change the effects of a link if the property is present.
	 * Note that duplicates are allowed and the final color set will override existing ones.
	 * @param identifier The identifier of the property
	 * @param color The color used to represent it 
	 */
	public void registerLinkProperty(String identifier, Color color);

	/**
	 * Returns an unmodifiable collection of all registered link properties
	 * @return An unmodifiable collection of the registered link properties
	 */
	public Collection<String> getLinkProperties();

	/**
	 * Retrieves the color associated with a link property flag
	 * @param identifier The link property
	 * @return The color associated with the link property
	 */
	public Color getLinkPropertyColor(String identifier);

	/**
	 * Returns a gradient which cycles through the colors assigned to the properties with lengths based on the weight of each property
	 * @param properties The property map to use
	 * @return A gradient of indeterminate length which will cycle through the properties by color
	 */
	public ColorGradient getPropertiesGradient(Map<String, Float> properties);

	/* 
	 * These functions are used for the ink mixing system.
	 * When a link panel is crafted from a pool of ink, each effect in the ink has its probability chance of being on the link panel.
	 * Adding items to the pool change the probabilities.
	 * Each additive has some set of effects with a paired probability.  When it is added to the ink, those probabilities are added to the ink after scaling the existing probabilities.
	 * The effect probabilities in the ink are scaled based on the "free" probability of the additive, or the inverse of the sum of the probabilities of all effects on the additive.
	 * 
	 * A concrete example:
	 * The ink pool currently has a 20% chance of adding disarm to the linkpanel.
	 * An additive is added with a 10% chance of intra-linking and a 10% chance of disarm.
	 * The sum of the additive's probabilities is 20%.  The inverse of which is 80%.
	 * This means the ink pool's probabilities are scaled to "fit" in the 80% (.2*.8 = .16)
	 * The additives probabilities are then added straight, resulting in the pool having a 26% chance of disarm and a 10% chance of intra-linking.
	 * Note that these effects are selected independently when crafting, so multiple effects are possible.
	 * Also note that the maximum total probability is 100%.
	 * Additives cannot add more than 100% probability total.
	 */

	/**
	 * Used to add an effect probability to an item.
	 * Note that this adds the effect along side any other effects already on the item.
	 * If the item would already add this property, the probability for that property is increased by the amount passed.
	 * The total probability of the effects on the item cannot exceed 1.
	 * When the item is added to the mixture the probabilities in the ink will scale to fit in the "free" portion of the probability.
	 * Properties bound to an itemstack override properties bound to ore dictionary names or item ids.
	 * @param itemstack The itemstack to match (exactly).  Will use a stackSize=1 copy.
	 * @param property The id of the property to add
	 * @param probability The probability strength of the property to add to the mixture
	 */
	public void addPropertyToItem(ItemStack itemstack, String property, float probability);
	/**
	 * Used to add an effect probability to an item.
	 * Note that this adds the effect along side any other effects already on the item.
	 * If the item would already add this property, the probability for that property is increased by the amount passed.
	 * The total probability of the effects on the item cannot exceed 1.
	 * When the item is added to the mixture the probabilities in the ink will scale to fit in the "free" portion of the probability.
	 * Properties bound to an ore dictionary name override properties bound to item ids.
	 * @param name The ore dictionary name to match
	 * @param property The id of the property to add
	 * @param probability The probability strength of the property to add to the mixture
	 */
	public void addPropertyToItem(String name, String property, float probability);
	/**
	 * Used to add an effect probability to an item.
	 * Note that this adds the effect along side any other effects already on the item.
	 * If the item would already add this property, the probability for that property is increased by the amount passed.
	 * The total probability of the effects on the item cannot exceed 1.
	 * When the item is added to the mixture the probabilities in the ink will scale to fit in the "free" portion of the probability.
	 * @param itemId The item id to match
	 * @param property The id of the property to add
	 * @param probability The probability strength of the property to add to the mixture
	 */
	public void addPropertyToItem(int itemId, String property, float probability);

	/**
	 * Retrieves the properties that an item will add if thrown into an ink mixer.
	 * @param itemstack The itemstack to use
	 * @return An unmodifiable map of the properties bound to an item or null if no mappings are found
	 */
	public Map<String, Float> getPropertiesForItem(ItemStack itemstack);
}
