package CustomOreGen.Server;


import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;


/**
 * customoregen:cog_feature. Attached to the last generation step (top_layer_modification) of every
 * biome by the biome modifiers in data/customoregen/neoforge/biome_modifier, and placed once per chunk
 * (its placed feature has no placement modifiers, so the origin is the chunk's corner). Being last means
 * this chunk's own feature writes are finished; see ServerState.onChunkFeaturesDone for the rest.
 */
public record LevelGenFeature() implements Feature {
	public static final MapCodec<LevelGenFeature> CODEC = MapCodec.unit(LevelGenFeature::new);

	@Override
	public MapCodec<LevelGenFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel region, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		ServerLevel level = region.getLevel();
		ServerState.checkIfServerChanged(level.getServer(), level.getServer().getWorldData());

		ChunkPos chunk = ChunkPos.containing(origin);
		ServerState.onChunkFeaturesDone(region, chunk.x(), chunk.z());
		return true;
	}
}
