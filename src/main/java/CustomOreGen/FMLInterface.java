package CustomOreGen;

import java.util.Random;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

@Mod("customoregen")
public class FMLInterface implements IWorldGenerator
{
    public static FMLInterface instance;
    
    public FMLInterface() {
    	instance = this;
    }
    
    /*@EventHandler
    public void onFMLPreInit(FMLPreInitializationEvent event)
    {
        CustomOreGenBase.log = event.getModLog();
        GameRegistry.registerWorldGenerator(this, Integer.MAX_VALUE);
        ForgeInterface.createAndRegister();
        CustomPacketPayload.registerChannels(new CustomPacketPayloadHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onFMLPostInit(FMLPostInitializationEvent event)
    {
    	CustomOreGenBase.onModPostLoad();
    }
    
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(event.getServer(), (WorldInfo)null);    
        registerCommands(event);
    }

    private void registerCommands(FMLServerStartingEvent event) {
    	CustomOreGenBase.log.debug("Registering Console command interface ...");
        ConsoleCommands commands = new ConsoleCommands();
        
        for (Method method : ConsoleCommands.class.getMethods()) {
        	if (method.getAnnotation(CommandDelegate.class) != null)
            {
                event.registerServerCommand(new ConsoleCommand(commands, method));
            }
        }
	}*/

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, ChunkGenerator chunkGenerator, AbstractChunkProvider chunkProvider) {
		// TODO Auto-generated method stub
        ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, chunkX, chunkZ, random);
	}

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
    	if (event.phase == TickEvent.Phase.END) {
    		ServerState.checkIfServerChanged(FMLCommonHandler.instance().getMinecraftServerInstance(), (WorldInfo)null);
    	}
    }

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
