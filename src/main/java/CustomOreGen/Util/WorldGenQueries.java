package CustomOreGen.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Height and biome lookups that are safe to make from inside world generation.
 *
 * Distribution layouts are computed for origin chunks up to several chunks away from the chunk
 * being generated. Asking the world (or a WorldGenRegion) about those positions would either throw
 * (outside the region) or block waiting on a chunk that is not generated yet. Instead, ask the
 * chunk generator, which computes terrain height and biomes from noise without loading anything.
 */
public final class WorldGenQueries
{
	private WorldGenQueries() {}

	public static ServerLevel serverLevel(LevelAccessor world)
	{
		if (world instanceof ServerLevel level)
		{
			return level;
		}
		if (world instanceof WorldGenLevel region)
		{
			return region.getLevel();
		}
		return null;
	}

	public static int getSurfaceHeight(LevelAccessor world, int x, int z)
	{
		ServerLevel level = serverLevel(world);
		if (level == null)
		{
			return world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z)).getY();
		}
		ServerChunkCache source = level.getChunkSource();
		return source.getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, source.randomState());
	}

	public static Biome getBiome(LevelAccessor world, int x, int y, int z)
	{
		ServerLevel level = serverLevel(world);
		if (level == null)
		{
			return world.getBiome(new BlockPos(x, y, z)).value();
		}
		ServerChunkCache source = level.getChunkSource();
		return source.getGenerator().getBiomeSource().createUncachedResolver(source.randomState())
				.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z))
				.value();
	}
}
