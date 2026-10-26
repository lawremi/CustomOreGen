package CustomOreGen.Server;

import java.awt.Frame;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.CogOreGenEvent;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.SimpleProfiler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.neoforge.common.NeoForge;

public class ServerState
{
    private static MinecraftServer _server = null;
    // Read from several worldgen threads at once.
    private static Map<LevelAccessor,WorldConfig> _worldConfigs = new ConcurrentHashMap<LevelAccessor, WorldConfig>();
    // Chunks whose FEATURES step has reached COG this session, per dimension. See onChunkFeaturesDone.
    private static final Map<ServerLevel,LongSet> _featuresDone = new HashMap<ServerLevel, LongSet>();
    private static int _populatedChunkCount = 0;
    private static boolean forcingChunk;

    public static WorldConfig getWorldConfig(LevelAccessor world)
    {
        WorldConfig cfg = _worldConfigs.get(world);
        return cfg != null ? cfg : loadWorldConfig(world);
    }

    /** True if a config is already loaded for this world and it has debugging mode on. Never loads one. */
    public static boolean isDebuggingMode(LevelAccessor world)
    {
        WorldConfig cfg = world == null ? null : _worldConfigs.get(world);
        return cfg != null && cfg.debuggingMode;
    }

    private static synchronized WorldConfig loadWorldConfig(LevelAccessor world)
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

    public static void clearWorldConfig(Level world)
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

    public static void validateDistributions(Collection<IOreDistribution> distributions, boolean cull) throws IllegalStateException
    {
        Iterator<IOreDistribution> it = distributions.iterator();

        while (it.hasNext())
        {
            IOreDistribution dist = it.next();

            if (!dist.validate() && cull)
            {
                it.remove();
            }
        }
    }

    public static void validateOptions(Collection<ConfigOption> options, boolean cull)
    {
        Iterator<ConfigOption> it = options.iterator();

        while (it.hasNext())
        {
            ConfigOption option = (ConfigOption)it.next();

            if (cull && option instanceof ConfigOption.DisplayGroup)
            {
                it.remove();
            }
        }
    }

    // TODO: add force option; if true then we will generate even when there is no version
    public static void populateDistributions(Collection<IOreDistribution> distributions, LevelAccessor world, int chunkX, int chunkZ)
    {
        SimpleProfiler.globalProfiler.startSection("Populate");
        
        // FIXME: Before 1.14, we were forcing instant block updates here. Not clear how to do that now.
        
        for (IOreDistribution dist : distributions) {
        	dist.generate(world, chunkX, chunkZ);
        	dist.populate((WorldGenLevel)world, chunkX, chunkZ);
            dist.cull();
        }
        
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
                LinkedList<GeometryStream> streams = new LinkedList<GeometryStream>();

                for (IOreDistribution dist : cfg.getOreDistributions())
                {
                    dist.generate(request.world, request.chunkX, request.chunkZ);
                    GeometryStream stream = dist.getDebuggingGeometry(request.world, request.chunkX, request.chunkZ);

                    if (stream != null)
                    {
                        streams.add(stream);
                    }
                    dist.cull();
                }

                return new GeometryData(request, streams);
            }
        }
    }

    /**
     * Called from LevelGenFeature as the last feature of a chunk's FEATURES step, with that chunk's
     * WorldGenRegion. This is COG's deferred population rule for 1.18+.
     *
     * During its FEATURES step a chunk may write into any chunk of its own 3x3, so a chunk's blocks are
     * not final (vanilla and other mods' ore blobs can still spill into it) until every chunk in its 3x3
     * has finished FEATURES. COG must run after that: Substitute distributions remove vanilla ore, and
     * then COG places its own. So each chunk marks itself done here, then populates every chunk in its
     * 3x3 whose whole 3x3 is now done. Exactly one chunk is the last of any given 3x3, so every chunk is
     * populated exactly once; and it always happens before that chunk can light (LIGHT requires its 3x3
     * at FEATURES), so before any player sees it.
     *
     * Population writes only into the chunk being populated, which is always within the generating
     * chunk's 3x3 write area. Structure layout reaches further, but only reads the seed and generator.
     */
    public static void onChunkFeaturesDone(WorldGenLevel region, int chunkX, int chunkZ)
    {
        ServerLevel level = region.getLevel();
        WorldConfig cfg = getWorldConfig(level);

        // Serialized so that two chunks of the same 3x3 finishing at once cannot both miss being last.
        synchronized (_featuresDone)
        {
            LongSet done = _featuresDone.computeIfAbsent(level, k -> new LongOpenHashSet());
            done.add(ChunkPos.pack(chunkX, chunkZ));

            for (int dX = -1; dX <= 1; ++dX)
            {
                for (int dZ = -1; dZ <= 1; ++dZ)
                {
                    if (neighborhoodFeaturesDone(region, done, chunkX + dX, chunkZ + dZ))
                    {
                        populateDistributions(cfg.getOreDistributions(), region, chunkX + dX, chunkZ + dZ);
                        postOreGenEvent(region, chunkX + dX, chunkZ + dZ);

                        if (++_populatedChunkCount % 64 == 0)
                        {
                            logPopulationSummary(level, cfg);
                        }
                    }
                }
            }
        }
    }

    /** Tells other mods (such as ore indicator plants) that a chunk's ores are final. */
    private static void postOreGenEvent(WorldGenLevel region, int chunkX, int chunkZ)
    {
        ChunkAccess chunk = region.getChunk(chunkX, chunkZ);
        RandomSource rand = RandomSource.create(region.getSeed() ^ ChunkPos.pack(chunkX, chunkZ));
        NeoForge.EVENT_BUS.post(new CogOreGenEvent(region, chunk, rand));
    }

    private static void logPopulationSummary(ServerLevel level, WorldConfig cfg)
    {
        StringBuilder sb = new StringBuilder();
        for (IOreDistribution dist : cfg.getOreDistributions())
        {
            Object placed = dist.getDistributionSetting("placedBlocks");
            if (placed instanceof Number n && n.longValue() > 0)
            {
                sb.append(' ').append(dist).append('=').append(n.longValue());
            }
        }
        CustomOreGenBase.log.debug("Populated " + _populatedChunkCount + " chunks (latest in " + level.dimension().identifier() + "); blocks placed:" + (sb.length() == 0 ? " none" : sb));
    }

    private static boolean neighborhoodFeaturesDone(WorldGenLevel region, LongSet done, int chunkX, int chunkZ)
    {
        for (int iX = chunkX - 1; iX <= chunkX + 1; ++iX)
        {
            for (int iZ = chunkZ - 1; iZ <= chunkZ + 1; ++iZ)
            {
                if (done.contains(ChunkPos.pack(iX, iZ)))
                {
                    continue;
                }
                // Finished in an earlier session (or before this chunk's step began). The FEATURES region
                // holds chunks out to 8 away, so these 5x5 lookups never leave it.
                ChunkAccess chunk = region.getChunk(iX, iZ, ChunkStatus.EMPTY, false);
                if (chunk == null || !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES))
                {
                    return false;
                }
            }
        }
        return true;
    }

	public static boolean checkIfServerChanged(MinecraftServer currentServer, WorldData worldInfo)
    {
        if (_server == currentServer)
        {
            return false;
        }
        else
        {
            if (currentServer != null && worldInfo == null)
            {
            	worldInfo = currentServer.getWorldData();
            }

            if (worldInfo == null)
            {
                return false;
            }

            onServerChanged(currentServer, worldInfo);
            return true;
        }
    }

    public static void onServerChanged(MinecraftServer server, WorldData worldInfo)
    {
        _worldConfigs.clear();
        synchronized (_featuresDone)
        {
            _featuresDone.clear();
        }
        WorldConfig.loadedOptionOverrides[1] = WorldConfig.loadedOptionOverrides[2] = null;

        _server = server;
        CustomOreGenBase.log.debug("Server world changed to " + worldInfo.getLevelName());
		/*File f = new File(_server.getLevel( Level.OVERWORLD).getSaveHandler().getLevelDirectory().getAbsolutePath());
        WorldConfig config = null;

        while (config == null)
        {
            try
            {
                config = new WorldConfig(worldInfo, f);
                validateOptions(config.getConfigOptions(), false);
                validateDistributions(config.getOreDistributions(), false);
            }
            catch (Exception var7)
            {
                if (!onConfigError(var7))
                {
                    break;
                }

                config = null;
            }
        }*/
    }

	public static void chunkForced(Level world, ChunkPos location) {
		if (forcingChunk) { // prevent infinite recursion when there are multiple chunk loaders
			return;
		}
		forcingChunk = true;
		
		WorldConfig cfg = getWorldConfig(world);
		int radius = (cfg.deferredPopulationRange + 15) / 16;
        
        for (int cX = location.x() - radius; cX <= location.x() + radius; ++cX)
        {
            for (int cZ = location.z() - radius; cZ <= location.z() + radius; ++cZ)
            {
            	if (cX != location.x() && cZ != location.z()) {
            		world.getChunk(cX, cZ);
            	}
            }
        }
        
        forcingChunk = false;
	}
}
