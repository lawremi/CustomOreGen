package CustomOreGen;

import java.util.Random;

import javax.annotation.Nonnull;

import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.WorldGenFeature;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

@EventBusSubscriber(modid = CustomOreGenBase.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusSubscriber {
    /** FIXME: Used to be called by registering this as an IWorldGenerator. What now?<br>
     *        This does two things: it first generates the distribution structures, without actually populating them.
     *        It then populates this chunk or any of its neighboring chunks if they now have a complete neighborhood.
     *        <br><br>
     *        The generation step could happen through the new Feature system, where each structure is a Feature.
     *        However, the population step falls outside of the system. There could be a populating Feature that runs after the rest.
     *        Better would be a Forge event when a chunk changes status. Population could run after FEATURES completion.
     */
	@Deprecated
	@SuppressWarnings("rawtypes")
	public void generate(Random random, int chunkX, int chunkZ, World world, ChunkGenerator chunkGenerator, AbstractChunkProvider chunkProvider) {
		ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, chunkX, chunkZ, random);
	}

    @SubscribeEvent
	public static void onFMLPreInit(FMLCommonSetupEvent event) {
    	CustomPacketPayloadHandler.register();
    }
    
    /**
     * This Feature attempts to replicate what {@link ModEventBusSubscriber#generate} did previously.
     */
    @SubscribeEvent
	public static void registerFeature(@Nonnull final RegistryEvent.Register<Feature<?>> event) {
    	event.getRegistry().register(new WorldGenFeature(NoFeatureConfig::deserialize).setRegistryName(new ResourceLocation(CustomOreGenBase.MODID, "cog_feature")));
	}
    
    @SubscribeEvent
	public static void onFMLPostInit(FMLLoadCompleteEvent event) {
    	CustomOreGenBase.onModPostLoad();
    }
}
