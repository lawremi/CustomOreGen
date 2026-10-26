package CustomOreGen.Server;

import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * customoregen:cog_feature. Attached to the last generation step (top_layer_modification) of every
 * biome by the biome modifiers in data/customoregen/neoforge/biome_modifier, and placed once per chunk
 * (its placed feature has no placement modifiers, so the origin is the chunk's corner). Being last means
 * this chunk's own feature writes are finished; see ServerState.onChunkFeaturesDone for the rest.
 */
public class LevelGenFeature extends Feature<NoneFeatureConfiguration> {

	public LevelGenFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel region = context.level();
		ServerLevel level = region.getLevel();
		ServerState.checkIfServerChanged(level.getServer(), level.getServer().getWorldData());

		ChunkPos chunk = new ChunkPos(context.origin());
		ServerState.onChunkFeaturesDone(region, chunk.x, chunk.z);
		return true;
	}
}
