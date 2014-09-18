package CustomOreGen.Server;

import java.awt.Frame;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.block.BlockSand;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveFormatOld;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.MystcraftSymbolData;
import CustomOreGen.Server.GuiCustomOreGenSettings.GuiOpenMenuButton;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.SimpleProfiler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ServerState
{
    private static MinecraftServer _server = null;
    private static Map<World,WorldConfig> _worldConfigs = new HashMap();
    private static Map<Integer,Map<ChunkCoordIntPair,int[]>> _populatedChunks = new HashMap();
    private static Object _optionsGuiButton = null;

    private static boolean isChunkSavedPopulated(World world, int chunkX, int chunkZ)
    {
        File saveFolder = getWorldConfig(world).dimensionDir;
        DataInputStream stream = RegionFileCache.getChunkInputStream(saveFolder, chunkX, chunkZ);

        if (stream != null)
        {
            try
            {
                NBTTagCompound ex = CompressedStreamTools.read(stream);

                if (ex.hasKey("Level") && ex.getCompoundTag("Level").getBoolean("TerrainPopulated"))
                {
                    return true;
                }
            }
            catch (IOException var6)
            {
                ;
            }
        }

        return false;
    }

    public static WorldConfig getWorldConfig(World world)
    {
        WorldConfig cfg = _worldConfigs.get(world);

        while (cfg == null)
        {
            try
            {
                cfg = new WorldConfig(world);
                validateOptions(cfg.getConfigOptions(), true);
                validateDistributions(cfg.getOreDistributions(), true);
            }
            catch (Exception var4)
            {
                if (onConfigError(var4))
                {
                    cfg = null;
                    continue;
                }

                cfg = WorldConfig.createEmptyConfig();
            }

            _worldConfigs.put(world, cfg);
        }

        return cfg;
    }

    public static void clearWorldConfig(World world)
    {
        _worldConfigs.remove(world);
    }

    public static boolean onConfigError(Throwable error)
    {
        CustomOreGenBase.log.error("Problem loading world config", error);
        Frame[] frames = Frame.getFrames();

        if (frames != null && frames.length > 0)
        {
            switch ((new ConfigErrorDialog()).showDialog(frames[0], error))
            {
                case 1:
                    return true;

                case 2:
                    return false;
            }
        }

        return false;
    }

    public static void validateDistributions(Collection distributions, boolean cull) throws IllegalStateException
    {
        Iterator it = distributions.iterator();

        while (it.hasNext())
        {
            IOreDistribution dist = (IOreDistribution)it.next();

            if (!dist.validate() && cull)
            {
                it.remove();
            }
        }
    }

    public static void validateOptions(Collection options, boolean cull)
    {
        Iterator it = options.iterator();

        while (it.hasNext())
        {
            ConfigOption option = (ConfigOption)it.next();

            if (cull && option instanceof ConfigOption.DisplayGroup)
            {
                it.remove();
            }
        }
    }

    public static void populateDistributions(Collection<IOreDistribution> distributions, World world, int chunkX, int chunkZ)
    {
        SimpleProfiler.globalProfiler.startSection("Populate");
        BlockSand.fallInstantly = true;
        world.scheduledUpdatesAreImmediate = true;
        
        for (IOreDistribution dist : distributions) {
        	dist.generate(world, chunkX, chunkZ);
            dist.populate(world, chunkX, chunkZ);
            dist.cull();
        }
        
        world.scheduledUpdatesAreImmediate = false;
        BlockSand.fallInstantly = false;
        SimpleProfiler.globalProfiler.endSection();
    }

    public static GeometryData getDebuggingGeometryData(GeometryRequestData request)
    {
        if (_server == null)
        {
            return null;
        }
        else if (request.world == null)
        {
            return null;
        }
        else
        {
            WorldConfig cfg = getWorldConfig(request.world);

            if (!cfg.debuggingMode)
            {
                return null;
            }
            else
            {
                int geomSize = 0;
                LinkedList streams = new LinkedList();

                for (IOreDistribution dist : cfg.getOreDistributions())
                {
                    dist.generate(request.world, request.chunkX, request.chunkZ);
                    GeometryStream stream = dist.getDebuggingGeometry(request.world, request.chunkX, request.chunkZ);

                    if (stream != null)
                    {
                        streams.add(stream);
                        geomSize += stream.getStreamDataSize();
                    }
                    dist.cull();
                }

                return new GeometryData(request, streams);
            }
        }
    }

    public static void onPopulateChunk(World world, int chunkX, int chunkZ) {
    	WorldConfig cfg = getWorldConfig(world);
    	int range = (cfg.deferredPopulationRange + 15) / 16;
    	for (int iX = chunkX - range; iX <= chunkX + range; ++iX)
        {
            for (int iZ = chunkZ - range; iZ <= chunkZ + range; ++iZ)
            {
            	if (allNeighborsPopulated(world, iX, iZ, range)) {
            		//CustomOreGenBase.log.info("[" + iX + "," + iZ + "]: POPULATING");
            		populateDistributions(cfg.getOreDistributions(), world, iX, iZ);
            	}
            }
        }
    }

    private static boolean allNeighborsPopulated(World world, int chunkX, int chunkZ, int range) {
    	int area = 4 * range * (range + 1) + 1;
    	int neighborCount = 0;
        for (int iX = chunkX - range; iX <= chunkX + range; ++iX)
        {
            for (int iZ = chunkZ - range; iZ <= chunkZ + range; ++iZ)
            {
            	if (chunkHasBeenPopulated(world, iX, iZ)) 
            	{
            		//CustomOreGenBase.log.info("[" + iX + "," + iZ + "]: populated neighbor");
            		neighborCount++;
            	}
            }
        }
		return neighborCount == area; 
	}

	private static boolean chunkHasBeenPopulated(World world, int chunkX, int chunkZ) {
		return chunkHasBeenGenerated(world, chunkX, chunkZ) && 
			world.getChunkFromChunkCoords(chunkX, chunkZ).isTerrainPopulated;
	}

	private static boolean chunkHasBeenGenerated(World world, int chunkX, int chunkZ) {
		if (world.getChunkProvider().chunkExists(chunkX, chunkZ)) {
			//CustomOreGenBase.log.info("[" + chunkX + "," + chunkZ + "]: loaded"); 
			return true;
		} else if (world.getChunkProvider() instanceof ChunkProviderServer) {
			IChunkLoader loader = ((ChunkProviderServer)world.getChunkProvider()).currentChunkLoader;
			if (loader instanceof AnvilChunkLoader) {
				//if (((AnvilChunkLoader) loader).chunkExists(world, chunkX, chunkZ))
				//	CustomOreGenBase.log.info("[" + chunkX + "," + chunkZ + "]: saved on disk");
				return ((AnvilChunkLoader) loader).chunkExists(world, chunkX, chunkZ);
			}
		}
		return false;
	}

	public static boolean checkIfServerChanged(MinecraftServer currentServer, WorldInfo worldInfo)
    {
        if (_server == currentServer)
        {
            return false;
        }
        else
        {
            if (currentServer != null && worldInfo == null)
            {
                if (currentServer.worldServers == null)
                {
                    return false;
                }

                for (WorldServer world : currentServer.worldServers) {
                    if (world != null)
                    {
                        worldInfo = world.getWorldInfo();
                    }

                    if (worldInfo != null)
                    {
                        break;
                    }                	
                }
                
                if (worldInfo == null)
                {
                    return false;
                }
            }

            onServerChanged(currentServer, worldInfo);
            return true;
        }
    }

    public static void onServerChanged(MinecraftServer server, WorldInfo worldInfo)
    {
        _worldConfigs.clear();
        WorldConfig.loadedOptionOverrides[1] = WorldConfig.loadedOptionOverrides[2] = null;
        _populatedChunks.clear();

        _server = server;
        CustomOreGenBase.log.debug("Server world changed to " + worldInfo.getWorldName());
        BiomeGenBase[] worldBaseDir = BiomeGenBase.getBiomeGenArray();
        int saveFormat = worldBaseDir.length;

        File var8 = null;
        ISaveFormat var9 = _server.getActiveAnvilConverter();

        if (var9 != null && var9 instanceof SaveFormatOld)
        {
            var8 = ((SaveFormatOld)var9).savesDirectory;
        }

        var8 = new File(var8, _server.getFolderName());
        WorldConfig var10 = null;

        while (var10 == null)
        {
            try
            {
                var10 = new WorldConfig(worldInfo, var8);
                validateOptions(var10.getConfigOptions(), false);
                validateDistributions(var10.getOreDistributions(), false);
            }
            catch (Exception var7)
            {
                if (!onConfigError(var7))
                {
                    break;
                }

                var10 = null;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void onWorldCreationMenuTick(GuiCreateWorld gui)
    {
        if (gui == null)
        {
            _optionsGuiButton = null;
        }
        else
        {
            if (_optionsGuiButton == null)
            {
                WorldConfig.loadedOptionOverrides[0] = null;
                GuiCustomOreGenSettings button = new GuiCustomOreGenSettings(gui);
                _optionsGuiButton = new GuiOpenMenuButton(gui, 99, 0, 0, 150, 20, "Custom Ore Generation...", button);
            }

            GuiOpenMenuButton button1 = (GuiOpenMenuButton)_optionsGuiButton;
            Collection controlList = (Collection)ReflectionHelper.getPrivateValue(GuiScreen.class, gui, 4);

            if (!controlList.contains(button1))
            {
                button1.xPosition = (gui.width - button1.getWidth()) / 2;
                button1.yPosition = 165;
                controlList.add(button1);
            }

            button1.visible = !((Boolean)ReflectionHelper.getPrivateValue(GuiCreateWorld.class, gui, 11)).booleanValue();
        }
    }

    public static void onClientLogin(EntityPlayerMP player)
    {
        if (player.worldObj != null && CustomOreGenBase.hasMystcraft())
        {
            Iterator i = getWorldConfig(player.worldObj).getMystcraftSymbols().iterator();

            while (i.hasNext())
            {
                MystcraftSymbolData symbolData = (MystcraftSymbolData)i.next();
                (new CustomPacketPayload(PayloadType.MystcraftSymbolData, symbolData)).sendToClient(player);
            }
        }
    }

	public static void chunkForced(World world, ChunkCoordIntPair location) {
		WorldConfig cfg = getWorldConfig(world);
		int radius = (cfg.deferredPopulationRange + 15) / 16;
        
        for (int cX = location.chunkXPos - radius; cX <= location.chunkXPos + radius; ++cX)
        {
            for (int cZ = location.chunkZPos - radius; cZ <= location.chunkZPos + radius; ++cZ)
            {
            	world.getChunkFromChunkCoords(cX, cZ);
            }
        }
	}
}
