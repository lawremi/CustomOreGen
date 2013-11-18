package com.xcompwiz.mystcraft.api.event;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.event.Event;

/**
 * The base event for the meteor
 * Used as the abstract parent class for the meteor events
 */
public abstract class MeteorEvent extends Event {
	public final Entity meteor;

	private MeteorEvent(Entity meteor) {
		this.meteor = meteor;
	}

	/**
	 * The spawn event for the meteor
	 * Sent whenever a meteor is first created
	 */
	public static class MetorSpawn extends MeteorEvent {
		public MetorSpawn(Entity meteor) {
			super(meteor);
		}
	}

	/**
	 * The impact event for the meteor
	 * This is sent out after all of the explosions have occurred and the meteor still exists, effectively suspended in the middle of the crater
	 */
	public static class MetorImpact extends MeteorEvent {
		public MetorImpact(Entity meteor) {
			super(meteor);
		}
	}

	/**
	 * The explosion event for the meteor impact
	 * Note that there will be several of these per meteor impact, as the crater is a result of multiple explosions at different positions around the meteor
	 * The list of positions is the set of all blocks which were affected by the explosion
	 * Only called server side
	 */
	public static class MetorExplosion extends MeteorEvent {
		/** The list of blocks affected by the explosion */
		public final List<ChunkPosition> blocks;

		public MetorExplosion(Entity meteor, List<ChunkPosition> blocks) {
			super(meteor);
			this.blocks = blocks;
		}
	}
}
