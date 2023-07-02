package CustomOreGen.Server;

import java.util.Random;
import java.util.function.Function;

import com.mojang.datafixers.Dynamic;

import CustomOreGen.CustomOreGenBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.registries.ObjectHolder;

public class WorldGenFeature extends Feature<NoFeatureConfig> {

	@ObjectHolder(value = CustomOreGenBase.MODID+":cog_feature")
	public static final Feature<NoFeatureConfig> FEATURE = null;

	public WorldGenFeature(Function<Dynamic<?>, ? extends NoFeatureConfig> configFactoryIn) {
		super(configFactoryIn, false); //bool: do block updates or not
	}

	@Override
	public boolean place(IWorld worldIn, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        ServerState.checkIfServerChanged(worldIn.getWorld().getServer(), worldIn.getWorldInfo());
        ChunkPos p = new ChunkPos(pos);
        WorldConfig cfg = ServerState.getWorldConfig(worldIn.getWorld());
        ServerState.onPopulateChunk(cfg, worldIn, p.x, p.z, worldIn.getRandom()); //TOOD: should use IWorld, not World
		return false;
	}
}
