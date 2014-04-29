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
import java.util.Random;

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
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveFormatOld;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.CustomOreGenBase;
import CustomOreGen.CustomPacketPayload;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.MystcraftInterface;
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

                if (CustomOreGenBase.hasMystcraft())
                {
                    Iterator ex = cfg.getMystcraftSymbols().iterator();

                    while (ex.hasNext())
                    {
                        MystcraftSymbolData symbolData = (MystcraftSymbolData)ex.next();
                        MystcraftInterface.appyAgeSpecificCOGSymbol(symbolData);
                    }
                }
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
                IOreDistribution dist;

                for (Iterator i$ = cfg.getOreDistributions().iterator(); i$.hasNext(); dist.cull())
                {
                    dist = (IOreDistribution)i$.next();
                    dist.generate(request.world, request.chunkX, request.chunkZ);
                    GeometryStream stream = dist.getDebuggingGeometry(request.world, request.chunkX, request.chunkZ);

                    if (stream != null)
                    {
                        streams.add(stream);
                        geomSize += stream.getStreamDataSize();
                    }
                }

                return new GeometryData(request, streams);
            }
        }
    }

    /* For tracking which chunks have been populated, the server bins the chunks into 4x4 meta-chunks.
     * Each meta-chunk is represented by a 16 integer array, indexed by the local X coordinate of a 
     * given chunk. Each integer contains two bits of information for every chunk, by splitting the 
     * 4-byte integer into two 2-byte parts. The local Z coordinate indexes into
     * the 16 bits of each part. The least significant part indicates whether an attempt has been
     * made to populate the chunk, while the most significant indicates whether we have checked
     * if the chunk was marked populated in the save (presumably this is an expensive check and 
     * thus is memoized).
     */
    public static void onPopulateChunk(World world, Random rand, int chunkX, int chunkZ)
    {
        WorldConfig cfg = getWorldConfig(world);
        Map<ChunkCoordIntPair,int[]> dimChunkMap = null;
        int cRange = world.provider.dimensionId;
        dimChunkMap = _populatedChunks.get(cRange);

        if (dimChunkMap == null)
        {
            dimChunkMap = new HashMap();
            _populatedChunks.put(cRange, dimChunkMap);
        }

        ChunkCoordIntPair neighborMax = new ChunkCoordIntPair(chunkX >>> 4, chunkZ >>> 4);
        int[] cX = dimChunkMap.get(neighborMax);

        if (cX == null)
        {
            cX = new int[16];
            dimChunkMap.put(neighborMax, cX);
        }

        cX[chunkX & 15] |= 65537 << (chunkZ & 15);
        int var16 = (cfg.deferredPopulationRange + 15) / 16;
        int var17 = 4 * var16 * (var16 + 1) + 1;

        for (int var18 = chunkX - var16; var18 <= chunkX + var16; ++var18)
        {
            for (int cZ = chunkZ - var16; cZ <= chunkZ + var16; ++cZ)
            {
                int neighborCount = 0;

                for (int iX = var18 - var16; iX <= var18 + var16; ++iX)
                {
                    for (int iZ = cZ - var16; iZ <= cZ + var16; ++iZ)
                    {
                        ChunkCoordIntPair chunkKey = new ChunkCoordIntPair(iX >>> 4, iZ >>> 4);
                        int[] chunkData = dimChunkMap.get(chunkKey);

                        if (chunkData == null)
                        {
                            chunkData = new int[16];
                            dimChunkMap.put(chunkKey, chunkData);
                        }

                        if ((chunkData[iX & 15] >>> (iZ & 15) & 65536) == 0)
                        {
                            boolean populated = isChunkSavedPopulated(world, iX, iZ);
                            //if (populated)
                            	//FMLLog.info("[%d/%d](%d/%d): populated in save", var18, cZ, iX, iZ);
                            chunkData[iX & 15] |= (populated ? 65537 : 65536) << (iZ & 15);
                        }

                        if ((chunkData[iX & 15] >>> (iZ & 15) & 1) != 0)
                        {
                        	//FMLLog.info("[%d/%d](%d/%d): is neighbor", var18, cZ, iX, iZ);
                        	++neighborCount;
                        }
                    }
                }

                if (neighborCount == var17)
                {
                	//FMLLog.info("[%d/%d]: populating", var18, cZ);
                    populateDistributions(cfg.getOreDistributions(), world, var18, cZ);
                } else {
                	//FMLLog.info("[%d/%d]: only %d neighbors", var18, cZ, neighborCount);
                }
            }
        }
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

        if (CustomOreGenBase.hasMystcraft())
        {
            MystcraftInterface.clearCOGSymbols();
        }

        _server = server;
        CustomOreGenBase.log.debug("Server world changed to " + worldInfo.getWorldName());
        BiomeGenBase[] worldBaseDir = BiomeGenBase.biomeList;
        int saveFormat = worldBaseDir.length;

        File var8 = null;
        ISaveFormat var9 = _server.getActiveAnvilConverter();

        if (var9 != null && var9 instanceof SaveFormatOld)
        {
            var8 = (File)ReflectionHelper.getPrivateValue(SaveFormatOld.class, (SaveFormatOld)var9, 0);
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

                if (CustomOreGenBase.hasMystcraft())
                {
                    Iterator var11 = var10.getMystcraftSymbols().iterator();

                    while (var11.hasNext())
                    {
                        MystcraftSymbolData symbolData = (MystcraftSymbolData)var11.next();
                        MystcraftInterface.addCOGSymbol(symbolData);
                    }
                }
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
            Collection controlList = (Collection)ReflectionHelper.getPrivateValue(GuiScreen.class, gui, 3);

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
