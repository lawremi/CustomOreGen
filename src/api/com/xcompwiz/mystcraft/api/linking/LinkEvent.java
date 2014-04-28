package com.xcompwiz.mystcraft.api.linking;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.Event;

/**
 * Mystcraft link (teleport) events
 * These events are all fired server side only
 * Client side functionality should be handled by listening on the server side and notifying the client via network message
 */
public abstract class LinkEvent extends Event {

	/** The world the entity is leaving.  May be null. */
	public final World origin;
	/** The destination world.  May be null. */
	public final World destination;
	/** The entity being linked */
	public final Entity entity;
	/** The link descriptor.  You should not modify this object in any way */
	public final ILinkInfo options;

	public LinkEvent(World origin, World destination, Entity entity, ILinkInfo info) {
		this.origin = origin;
		this.destination = destination;
		this.entity = entity;
		this.options = info;
	}

	/**
	 * Cancel this event to prevent linking.
	 */
    @Cancelable
	public static class LinkEventAllow extends LinkEvent {
		public LinkEventAllow(World origin, Entity entity, ILinkInfo info) {
			super(origin, null, entity, info);
		}
	}

	/**
	 * Used to provide alternate values for the link
	 */
	public static class LinkEventAlter extends LinkEvent {
		/** Set this to alter the link (null until set) */
		public ChunkCoordinates spawn;
		/** Set this to alter the link (null until set) */
		public Float rotationYaw;

		public LinkEventAlter(World origin, World destination, Entity entity, ILinkInfo info) {
			super(origin, destination, entity, info);
		}
	}

	/**
	 * Called before the entity is linked
	 */
	public static class LinkEventStart extends LinkEvent {
		public LinkEventStart(World origin, Entity entity, ILinkInfo info) {
			super(origin, null, entity, info);
		}
	}

	/**
	 * Called when the entity leaves their current world but has not yet entered the next 
	 */
	public static class LinkEventExitWorld extends LinkEvent {
		public LinkEventExitWorld(Entity entity, ILinkInfo info) {
			super(null, null, entity, info);
		}
	}

	/**
	 * Called when the entity enters the new world and their position has been approximated.
	 * The entity is not fully associated or tuned with the world yet, and players have not been sent 
	 * server-side information such as weather and time.
	 */
	public static class LinkEventEnterWorld extends LinkEvent {
		public LinkEventEnterWorld(World destination, Entity entity, ILinkInfo info) {
			super(null, destination, entity, info);
		}
	}

	/**
	 * Called once everything is finalized.
	 * The entity is fully realized in the world and has been updated on clients.
	 */
	public static class LinkEventEnd extends LinkEvent {
		public LinkEventEnd(World destination, Entity entity, ILinkInfo info) {
			super(null, destination, entity, info);
		}
	}
}
