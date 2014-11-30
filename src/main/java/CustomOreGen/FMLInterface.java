package CustomOreGen;

import java.lang.reflect.Method;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Util.ConsoleCommand;
import CustomOreGen.Util.ConsoleCommand.CommandDelegate;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid="CustomOreGen", useMetadata=true)
public class FMLInterface implements IWorldGenerator
{
    @Instance("CustomOreGen")
    public static FMLInterface instance;
    private Object _worldCreationGui = null;
    private long _serverTickCount = 0L;

    @EventHandler
    public void onFMLPreInit(FMLPreInitializationEvent event)
    {
        CustomOreGenBase.log = event.getModLog();
        GameRegistry.registerWorldGenerator(this, Integer.MAX_VALUE);
        ForgeInterface.createAndRegister();
        CustomPacketPayload.registerChannels(new CustomPacketPayloadHandler());
        FMLCommonHandler.instance().bus().register(this);
    }

    @EventHandler
    public void onFMLPostInit(FMLPostInitializationEvent event)
    {
    	CustomOreGenBase.onModPostLoad();
    }
    
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), (WorldInfo)null);    
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

	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, chunkX, chunkZ, random);
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
    	if (event.phase == TickEvent.Phase.END) {
    		ServerState.checkIfServerChanged(MinecraftServer.getServer(), (WorldInfo)null);
    		++this._serverTickCount;
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

        if (mc.theWorld == null && mc.currentScreen != null)
        {
            if (mc.currentScreen instanceof GuiCreateWorld)
            {
                if (this._worldCreationGui == null)
                {
                    this._worldCreationGui = mc.currentScreen;
                }

                ServerState.onWorldCreationMenuTick((GuiCreateWorld)mc.currentScreen);
            }
            else if (this._worldCreationGui != null && (mc.currentScreen instanceof GuiSelectWorld || mc.currentScreen instanceof GuiMainMenu))
            {
                this._worldCreationGui = null;
                ServerState.onWorldCreationMenuTick((GuiCreateWorld)null);
            }
        }
        else if (this._worldCreationGui != null)
        {
            this._worldCreationGui = null;
            ServerState.onWorldCreationMenuTick((GuiCreateWorld)null);
        }

        /* Still needed?
        if (mc.isSingleplayer())
        {
            this.onServerTick();
        }
        */

        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
        }
    }

    @SubscribeEvent
    public void onClientLogin(PlayerLoggedInEvent event)
    {
        World handlerWorld = event.player == null ? null : event.player.worldObj;
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), handlerWorld == null ? null : handlerWorld.getWorldInfo());

        if (event.player != null)
        {
            ServerState.onClientLogin((EntityPlayerMP)event.player);
        }
    }

	private static ModContainer getModContainer() {
		return FMLCommonHandler.instance().findContainerFor(instance);
	}

	public static String getDisplayString() {
		ModContainer metadata = FMLInterface.getModContainer();
    	return metadata.getName() + " " + metadata.getVersion();
	}
}
