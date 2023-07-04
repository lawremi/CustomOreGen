package CustomOreGen;

import java.util.Iterator;

import com.mojang.brigadier.CommandDispatcher;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.LevelGenFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelLastEvent;
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
    		biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, Biome.createDecoratedFeature(LevelGenFeature.FEATURE, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG));
    	}
    }
    
    private static void removeAllOreGenerators(TagKey<Block> tag, Biome biome) {
		Iterator<ConfiguredFeature<?>> it = biome.getFeatures(Decoration.UNDERGROUND_ORES).iterator();
		while(it.hasNext()) { //for each feature in that biome
			ConfiguredFeature<?> feature = it.next();
			if(feature.config instanceof DecoratedFeatureConfig) {
				DecoratedFeatureConfig dfconfig = (DecoratedFeatureConfig)feature.config;
				if(dfconfig.feature.config instanceof OreFeatureConfig) { //if it is an ore gen feature
					OreFeatureConfig oreConfigl = (OreFeatureConfig)dfconfig.feature.config;
					if(oreConfigl.state.isIn(tag)) {
						it.remove(); //remove it
					}
				}
				if(dfconfig.feature.config instanceof ReplaceBlockConfig) { //emerald ore uses this
					ReplaceBlockConfig oreConfigl = (ReplaceBlockConfig)dfconfig.feature.config;
					if(oreConfigl.state.isIn(tag)) {
						it.remove(); //remove it
					}
				}
				if(dfconfig.feature.config instanceof BlockBlobConfig) { //emerald ore uses this
					BlockBlobConfig oreConfigl = (BlockBlobConfig)dfconfig.feature.config;
					if(oreConfigl.state.isIn(tag)) {
						it.remove(); //remove it
					}
				}
				if(dfconfig.feature.config instanceof SphereReplaceConfig) { //emerald ore uses this
					SphereReplaceConfig oreConfigl = (SphereReplaceConfig)dfconfig.feature.config;
					if(oreConfigl.state.isIn(tag)) {
						it.remove(); //remove it
					}
				}
				//there are other implementors of IFeatureConfig
				//such as clay deposits (BlockWithContextConfig) that may also be of interest
			}
		}
	}
    
    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(event.getServer(), (WorldData)null);    
        registerCommands(event.getCommandDispatcher());
    }

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
        		handlerLevel.getLevelData());
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
            ServerState.checkIfServerChanged(sWorld.getServer(), sWorld.getLevelData());
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
