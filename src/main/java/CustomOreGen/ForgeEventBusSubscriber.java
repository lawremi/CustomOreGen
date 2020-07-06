package CustomOreGen;

import java.util.Iterator;

import com.mojang.brigadier.CommandDispatcher;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.WorldGenFeature;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.tags.Tag;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.feature.BlockBlobConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.ReplaceBlockConfig;
import net.minecraft.world.gen.feature.SphereReplaceConfig;
import net.minecraft.world.gen.placement.IPlacementConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
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
    		biome.addFeature(GenerationStage.Decoration.UNDERGROUND_DECORATION, Biome.createDecoratedFeature(WorldGenFeature.FEATURE, IFeatureConfig.NO_FEATURE_CONFIG, Placement.NOPE, IPlacementConfig.NO_PLACEMENT_CONFIG));
    	}
    }
    
    private static void removeAllOreGenerators(Tag<Block> tag, Biome biome) {
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
        ServerState.checkIfServerChanged(event.getServer(), (WorldInfo)null);    
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

        if (mc.world != null && ClientState.hasWorldChanged(mc.world))
        {
            ClientState.onWorldChanged(mc.world);
        }
    }

    @SubscribeEvent
    public static void onClientLogin(PlayerLoggedInEvent event)
    {
        World handlerWorld = event.getPlayer().world;
        ServerState.checkIfServerChanged(handlerWorld.getServer(), 
        		handlerWorld.getWorldInfo());
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderWorldLast(RenderWorldLastEvent event)
    {
        ClientState.onRenderWorld(Minecraft.getInstance().getRenderViewEntity(), (double)event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onLoadWorld(WorldEvent.Load event)
    {
    	World world = event.getWorld().getWorld();
        if (world instanceof ServerWorld)
        {
            ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
            ServerState.getWorldConfig(world);
        }
        else if (event.getWorld() instanceof ClientWorld && ClientState.hasWorldChanged(world))
        {
            ClientState.onWorldChanged(world);
        }
    }

    @SubscribeEvent
    public static void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.addOptionsButtonToGui((CreateWorldScreen)event.getGui(), event.getWidgetList());
        }
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.updateOptionsButtonVisibility((CreateWorldScreen)event.getGui());
        }
    }
}
