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
import net.minecraft.src.ModLoader;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.feature.WorldGenerator;
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
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ServerState
{
    private static MinecraftServer _server = null;
    private static Map _worldConfigs = new HashMap();
    private static Map _populatedChunks = new HashMap();
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

    private static void patchBiomeDecorator(BiomeDecorator decorator)
    {
        try
        {
            WorldGenerator ex = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 10);
            WorldGenerator ironGen = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 11);
            WorldGenerator goldGen = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 12);
            WorldGenerator redstoneGen = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 13);
            WorldGenerator diamondGen = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 14);
            WorldGenerator lapisGen = (WorldGenerator)ModLoader.getPrivateValue(BiomeDecorator.class, decorator, 15);
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 10, new WorldGenEmpty(ex));
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 11, new WorldGenEmpty(ironGen));
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 12, new WorldGenEmpty(goldGen));
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 13, new WorldGenEmpty(redstoneGen));
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 14, new WorldGenEmpty(diamondGen));
            ModLoader.setPrivateValue(BiomeDecorator.class, decorator, 15, new WorldGenEmpty(lapisGen));
        }
        catch (Exception var7)
        {
            CustomOreGenBase.log.throwing("CustomOreGenBase", "patchBiomeDecorator", var7);
        }
    }

    public static WorldConfig getWorldConfig(World world)
    {
        WorldConfig cfg = (WorldConfig)_worldConfigs.get(world);

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
        CustomOreGenBase.log.throwing("CustomOreGen.ServerState", "loadWorldConfig", error);
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

        ModLoader.throwException((String)null, error);
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

    public static void onPopulateChunk(World world, Random rand, int chunkX, int chunkZ)
    {
        WorldConfig cfg = getWorldConfig(world);
        Object dimChunkMap = null;
        Integer cRange = Integer.valueOf(world.provider.dimensionId);
        dimChunkMap = (Map)_populatedChunks.get(cRange);

        if (dimChunkMap == null)
        {
            dimChunkMap = new HashMap();
            _populatedChunks.put(cRange, dimChunkMap);
        }

        ChunkCoordIntPair neighborMax = new ChunkCoordIntPair(chunkX >>> 4, chunkZ >>> 4);
        int[] cX = (int[])((Map)dimChunkMap).get(neighborMax);

        if (cX == null)
        {
            cX = new int[16];
            ((Map)dimChunkMap).put(neighborMax, cX);
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
                        int[] chunkData = (int[])((Map)dimChunkMap).get(chunkKey);

                        if (chunkData == null)
                        {
                            chunkData = new int[16];
                            ((Map)dimChunkMap).put(chunkKey, chunkData);
                        }

                        if ((chunkData[iX & 15] >>> (iZ & 15) & 65536) == 0)
                        {
                            boolean populated = isChunkSavedPopulated(world, iX, iZ);
                            chunkData[iX & 15] |= (populated ? 65537 : 65536) << (iZ & 15);
                        }

                        if ((chunkData[iX & 15] >>> (iZ & 15) & 1) != 0)
                        {
                            ++neighborCount;
                        }
                    }
                }

                if (neighborCount == var17)
                {
                    populateDistributions(cfg.getOreDistributions(), world, var18, cZ);
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
        CustomOreGenBase.log.finer("Server world changed to " + worldInfo.getWorldName());
        BiomeGenBase[] worldBaseDir = BiomeGenBase.biomeList;
        int saveFormat = worldBaseDir.length;

        for (int cfg = 0; cfg < saveFormat; ++cfg)
        {
            BiomeGenBase ex = worldBaseDir[cfg];

            if (ex != null && ex.theBiomeDecorator != null)
            {
                patchBiomeDecorator(ex.theBiomeDecorator);
            }
        }

        File var8 = null;
        ISaveFormat var9 = _server.getActiveAnvilConverter();

        if (var9 != null && var9 instanceof SaveFormatOld)
        {
            var8 = (File)ModLoader.getPrivateValue(SaveFormatOld.class, (SaveFormatOld)var9, 0);
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
            Collection controlList = (Collection)ModLoader.getPrivateValue(GuiScreen.class, gui, 3);

            if (!controlList.contains(button1))
            {
                button1.xPosition = (gui.width - button1.getWidth()) / 2;
                button1.yPosition = 165;
                controlList.add(button1);
            }

            button1.drawButton = !((Boolean)ModLoader.getPrivateValue(GuiCreateWorld.class, gui, 11)).booleanValue();
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
                (new CustomPacketPayload(PayloadType.MystcraftSymbolData, symbolData)).sendToClient(player.playerNetServerHandler);
            }
        }
    }
}
