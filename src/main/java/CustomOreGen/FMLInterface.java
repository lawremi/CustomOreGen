package CustomOreGen;

import java.util.Random;

import org.apache.logging.log4j.LogManager;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod("customoregen")
public class FMLInterface
{
    public static FMLInterface instance;
    
    public FMLInterface() {
    	instance = this;
        CustomOreGenBase.log = LogManager.getLogger();
        ForgeInterface.createAndRegister(); //TODO: may need to go in the FMLCommonSetupEvent
        

        //TODO GameRegistry.registerWorldGenerator(this, Integer.MAX_VALUE);
        CustomPacketPayload.registerChannels(new CustomPacketPayloadHandler());
        MinecraftForge.EVENT_BUS.register(this);
    	
    	final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener((FMLCommonSetupEvent event) -> {
			
		});
		modEventBus.addListener((FMLLoadCompleteEvent event) -> {
	    	CustomOreGenBase.onModPostLoad();
		});
    }
    
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(event.getServer(), (WorldInfo)null);    
        //TODO: commands. won't be here like this
        //registerCommands(event);
    }

    /*private void registerCommands(FMLServerStartingEvent event) {
    	CustomOreGenBase.log.debug("Registering Console command interface ...");
        ConsoleCommands commands = new ConsoleCommands();
        
        for (Method method : ConsoleCommands.class.getMethods()) {
        	if (method.getAnnotation(CommandDelegate.class) != null)
            {
                event.registerServerCommand(new ConsoleCommand(commands, method));
            }
        }
	}*/

    //TODO: IWorldGenerator is no longer used...anywhere
	/*@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, ChunkGenerator chunkGenerator, AbstractChunkProvider chunkProvider) {
		ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, chunkX, chunkZ, random);
	}*/

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
    	if (event.phase == TickEvent.Phase.END) {
    		ServerState.checkIfServerChanged(ServerLifecycleHooks.getCurrentServer(), (WorldInfo)null);
    	}
    }

    public void onEvent(BreakEvent e) { }
    
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event)
    {
    	if (event.phase != TickEvent.Phase.END) {
    		return;
    	}
        Minecraft mc = Minecraft.getInstance();

        if (mc.world != null && ClientState.hasWorldChanged(mc.world))
        {
            ClientState.onWorldChanged(mc.world);
        }
    }

    @SubscribeEvent
    public void onClientLogin(PlayerLoggedInEvent event)
    {
        World handlerWorld = event.getPlayer().world;
        ServerState.checkIfServerChanged(handlerWorld.getServer(), 
        		handlerWorld.getWorldInfo());
    }

	private static ModContainer getModContainer() {
		return ModList.get().getModContainerByObject(instance).get();
	}

	public static String getDisplayString() {
		ModContainer metadata = FMLInterface.getModContainer();
    	return metadata.getModId() + " " + metadata.getModInfo().getVersion();
	}
}
