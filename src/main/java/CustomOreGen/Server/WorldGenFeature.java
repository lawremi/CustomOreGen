package CustomOreGen.Server;

import java.util.Random;
import java.util.function.Function;

import com.mojang.datafixers.Dynamic;

import CustomOreGen.CustomOreGenBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.level.ILevel;
import net.minecraft.level.level.levelgen.ChunkGenerator;
import net.minecraft.level.level.levelgen.GenerationSettings;
import net.minecraft.level.level.levelgen.feature.Feature;
import net.minecraft.level.level.levelgen.feature.NoFeatureConfig;
import net.minecraftforge.registries.ObjectHolder;

public class LevelGenFeature extends Feature<NoFeatureConfig> {

	@ObjectHolder(value = CustomOreGenBase.MODID+":cog_feature")
	public static final Feature<NoFeatureConfig> FEATURE = null;

	public LevelGenFeature(Function<Dynamic<?>, ? extends NoFeatureConfig> configFactoryIn) {
		super(configFactoryIn, false); //bool: do block updates or not
	}

	@Override
	public boolean place(ILevel worldIn, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        ServerState.checkIfServerChanged(worldIn.getLevel().getServer(), worldIn.getLevelInfo());
        ChunkPos p = new ChunkPos(pos);
        WorldConfig cfg = ServerState.getWorldConfig(worldIn.getLevel());
        ServerState.onPopulateChunk(cfg, worldIn, p.x, p.z, worldIn.getRandom()); //TOOD: should use ILevel, not Level
		return false;
	}
}
