package CustomOreGen;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(
        modid = "@MODID@",
        name = "@MODNAME@",
        version = "@VERSION@",
        acceptedMinecraftVersions = "@MCVERSION@",
        dependencies = "after:*;"
)
public class FMLInterface implements ITickHandler, IWorldGenerator
{
    @Instance("CustomOreGen")
    public static FMLInterface instance;
    private Object _worldCreationGui = null;
    private long _serverTickCount = 0L;

    @EventHandler
    public void onFMLPreInit(FMLPreInitializationEvent event)
    {
        CustomOreGenBase.log = event.getModLog();
        CustomOreGenBase.log.finer("Registering FML interface ...");
        TickRegistry.registerTickHandler(this, FMLCommonHandler.instance().getSide());
        GameRegistry.registerWorldGenerator(this);

        if (CustomOreGenBase.hasForge())
        {
            ForgeInterface.createAndRegister();
        }
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), (WorldInfo)null);
    }

    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
    {
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), world.getWorldInfo());
        ServerState.onPopulateChunk(world, random, chunkX, chunkZ);
    }

    public void tickStart(EnumSet type, Object ... tickData) {}

    public void tickEnd(EnumSet type, Object ... tickData)
    {
        if (type.contains(TickType.SERVER))
        {
            this.onServerTick();
        }

        if (type.contains(TickType.CLIENT))
        {
            this.onClientTick();
        }
    }

    private void onServerTick()
    {
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), (WorldInfo)null);
        ++this._serverTickCount;
    }

    @SideOnly(Side.CLIENT)
    private void onClientTick()
    {
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

        if (mc.isSingleplayer())
        {
            this.onServerTick();
        }

        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
        }
    }

    public EnumSet ticks()
    {
        return EnumSet.of(TickType.CLIENT, TickType.SERVER);
    }

    public String getLabel()
    {
        return "CustomOreGen.FMLInterface";
    }
}
