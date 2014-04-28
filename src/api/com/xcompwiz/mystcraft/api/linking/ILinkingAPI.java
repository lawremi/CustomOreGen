package com.xcompwiz.mystcraft.api.linking;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Functions for interfacing with the linking mechanics.
 * The implementation of this is provided by MystAPI.
 * See {@link LinkEvent} for events to allow you to alter a link itself
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface ILinkingAPI {

	/**
	 * Returns if a link is allowed.
	 * Note that this does not mean an attempted link will be successful.
	 * This only checks this entity, and not any entities attached to it.
	 * @param entity The entity to link
	 * @param linkinfo The link destination and information
	 * @return True if the link is permitted.
	 */
	boolean isLinkAllowed(Entity entity, ILinkInfo linkinfo);

	/**
	 * Attempts to link (Teleport) the entity using the information provided by the link info object.
	 * Any mounted or riding entities will be linked as well.
	 * This is not guaranteed to succeed.
	 * Note that the link can fail on individual entities in a mount chain but succeed on part of it (the rider may link without the mount, for example).
	 * @param entity The entity to link
	 * @param linkinfo The link destination and information. A valid link info object can be created using the other functions from this interface.
	 */
	void linkEntity(Entity entity, ILinkInfo linkInfo);

	/**
	 * Returns a tag compound which contains all of the information to link an entity back to the present location
	 * Defaults to Non-Intra-Age, so the link only works when changing dimensions
	 * Note that the Entity's yaw is maintained, but not its pitch
	 * @param worldObj The world to link to
	 * @param Entity An entity which holds the position and orientation desired.  Can be a dummy/fake entity.
	 * @return The Link's descriptor.  You can modify the link's properties through this.
	 */
	ILinkInfo createLinkInfoFromPosition(World worldObj, Entity location);

	/**
	 * Creates a new LinkInfo object from the data in a NBTTagCompound.
	 * This will not modify the compound and modifications made to the LinkInfo do not reflect in the compound.
	 * If null is passed in it returns a default link description (overworld spawn).
	 * This is primarily meant for use in conjunction with ILinkInfo's conversion to NBT function.
	 * @param linkInfo The link destination and information in NBT.  This can be obtained from an ILinkInfo object.
	 * @return The Link's descriptor.  You can modify the link's properties through this.
	 */
	ILinkInfo createLinkInfo(NBTTagCompound linkInfo);

	/**
	 * Returns the unique identifier for a dimension for use with the LinkInfo link descriptor object
	 * Setting the dimension value of a LinkInfo object to this value will set it to target the passed dimension
	 * @param worldObj The world object of the dimension to get the unique link id for
	 * @return The unique id of te dimension 
	 */
	int getDimensionUID(World worldObj);
}
