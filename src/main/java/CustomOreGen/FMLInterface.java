package CustomOreGen;

import java.lang.reflect.Method;
import java.util.Random;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Util.ConsoleCommand;
import CustomOreGen.Util.ConsoleCommand.CommandDelegate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid="customoregen", useMetadata=true, version="@VERSION@", acceptedMinecraftVersions="[1.10]")
public class FMLInterface implements IWorldGenerator
{
    @Instance("customoregen")
    public static FMLInterface instance;
    @EventHandler
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
	}


	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider)
    {
        ServerState.checkIfServerChanged(world.getMinecraftServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, chunkX, chunkZ, random);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
    	if (event.phase == TickEvent.Phase.END) {
    		ServerState.checkIfServerChanged(FMLCommonHandler.instance().getMinecraftServerInstance(), (WorldInfo)null);
    	}
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event)
    {
    	if (event.phase != TickEvent.Phase.END) {
    		return;
    	}
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
        }
    }

    @SubscribeEvent
    public void onClientLogin(PlayerLoggedInEvent event)
    {
        World handlerWorld = event.player.worldObj;
        ServerState.checkIfServerChanged(handlerWorld.getMinecraftServer(), 
        		handlerWorld.getWorldInfo());
    }

	private static ModContainer getModContainer() {
		return FMLCommonHandler.instance().findContainerFor(instance);
	}

	public static String getDisplayString() {
		ModContainer metadata = FMLInterface.getModContainer();
    	return metadata.getName() + " " + metadata.getVersion();
	}
}
