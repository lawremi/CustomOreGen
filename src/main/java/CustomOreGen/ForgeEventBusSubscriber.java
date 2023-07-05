package CustomOreGen;

import java.util.Iterator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = CustomOreGenBase.MODID)
public class ForgeEventBusSubscriber
{
	
	@SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event)
    {
    	for(Biome biome : ForgeRegistries.BIOMES) {
        	removeAllOreGenerators(Tags.Blocks.ORES, biome);
        	//UNDERGROUND_DECORATION happens after UNDERGROUND_ORES
        	//Uses Placement.NOPE IPlacementConfig.NO_PLACEMENT_CONFIG as the feature will handle itself
    		//biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, Biome.createDecoratedFeature(LevelGenFeature.FEATURE, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG));
        }
    }
    
    private static void removeAllOreGenerators(TagKey<Block> tag, Biome biome) {
    	List<HolderSet<PlacedFeature>> features = biome.getGenerationSettings().features();
    	Iterator<HolderSet<PlacedFeature>> featureit = features.iterator();
    	while(featureit.hasNext())
    	{
    		HolderSet<PlacedFeature> holder = featureit.next();
    		PlacedFeature feature = holder.get(Decoration.UNDERGROUND_ORES.ordinal()).get();
    		FeatureConfiguration cfg = feature.feature().get().config();
    		if(cfg instanceof OreConfiguration oreConfig) { //if it is an ore gen feature
    			Iterator<TargetBlockState> it = oreConfig.targetStates.iterator();
				while(it.hasNext()) {
					TargetBlockState st = it.next();
					if(st.state.is(tag)) {
						it.remove();
					}
				}
				if(oreConfig.targetStates.size() == 0) {
					featureit.remove();
				}
    		}
    		if(cfg instanceof ReplaceBlockConfiguration replaceConfig) {
    			Iterator<TargetBlockState> it = replaceConfig.targetStates.iterator();
				while(it.hasNext()) {
					TargetBlockState st = it.next();
					if(st.state.is(tag)) {
						it.remove();
					}
				}
				if(replaceConfig.targetStates.size() == 0) {
					featureit.remove();
				}
    		}
    		if(cfg instanceof ReplaceSphereConfiguration sphereConfig) {
    			if(sphereConfig.targetState.is(tag)) {
    				featureit.remove();
    			}
    		}
    	}
	}
    
    //TODO: Commands
    /*@SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(event.getServer(), (WorldData)null);    
        registerCommands(event.getCommandDispatcher());
    }*/

    @SuppressWarnings("unused")
	private static void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
    	CustomOreGenBase.log.debug("Registering Console command interface ...");
        ConsoleCommands.register(dispatcher);
	}

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event)
    {
    	if (event.phase != Phase.END) {
    		return;
    	}
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && ClientState.hasLevelChanged(mc.level))
        {
            ClientState.onLevelChanged(mc.level);
        }
    }

    @SubscribeEvent
    public static void onClientLogin(PlayerLoggedInEvent event)
    {
        Level handlerLevel = event.getEntity().level;
        ServerState.checkIfServerChanged(handlerLevel.getServer(), 
        		handlerLevel.getServer().getWorldData());
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderLevelLast(RenderLevelStageEvent event)
    {
        ClientState.onRenderLevel(Minecraft.getInstance().getCameraEntity(), (double)event.getPartialTick());
    }

    @SubscribeEvent
    public static void onLoadLevel(LevelEvent.Load event)
    {
    	LevelAccessor world = event.getLevel();
        if (world instanceof ServerLevel sWorld)
        {
            ServerState.checkIfServerChanged(sWorld.getServer(), sWorld.getServer().getWorldData());
            ServerState.getWorldConfig(sWorld);
        }
        else if (event.getLevel() instanceof ClientLevel cWorld && ClientState.hasLevelChanged(cWorld))
        {
            ClientState.onLevelChanged(cWorld);
        }
    }

    @SubscribeEvent
    public static void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            ServerState.addOptionsButtonToGui((CreateWorldScreen)event.getScreen(), event.getListenersList(), event::addListener);
        }
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onActionPerformed(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            ServerState.updateOptionsButtonVisibility((CreateWorldScreen)event.getScreen());
        }
    }
}
