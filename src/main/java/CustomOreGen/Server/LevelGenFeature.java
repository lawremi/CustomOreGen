package CustomOreGen.Server;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LevelGenFeature extends Feature<NoneFeatureConfiguration> {

	//@ObjectHolder(value = CustomOreGenBase.MODID+":cog_feature")
	//public static final Feature<NoneFeatureConfiguration> FEATURE = null;
	
	public LevelGenFeature(Codec<NoneFeatureConfiguration> p_65786_) {
		super(p_65786_);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		//context.chunkGenerator();
		WorldConfig cfg = ServerState.getWorldConfig(context.level());
		return false;
	}

	//ChunkAccess
	/*@Override
	public boolean place(LevelAccessor worldIn, ChunkGenerator<? extends GenerationSettings> generator, RandomSource rand, BlockPos pos, NoneFeatureConfiguration config) {
        ServerState.checkIfServerChanged(worldIn.getLevel().getServer(), worldIn.getLevelInfo());
        ChunkPos p = new ChunkPos(pos);
        WorldConfig cfg = ServerState.getWorldConfig(worldIn.getLevel());
        ServerState.onPopulateChunk(cfg, worldIn, p.x, p.z, worldIn.getRandom()); //TOOD: should use LevelAccessor, not Level
		return false;
	}*/
}
