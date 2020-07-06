package CustomOreGen.Server;

import java.awt.Frame;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.SimpleProfiler;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ServerState
{
    private static MinecraftServer _server = null;
    private static Map<World,WorldConfig> _worldConfigs = new HashMap<World, WorldConfig>();
    private static Button _optionsGuiButton = null;
    private static boolean forcingChunk;

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
    public static void populateDistributions(Collection<IOreDistribution> distributions, World world, int chunkX, int chunkZ)
    {
        SimpleProfiler.globalProfiler.startSection("Populate");
        
        // FIXME: Before 1.14, we were forcing instant block updates here. Not clear how to do that now.
        
        for (IOreDistribution dist : distributions) {
        	dist.generate(world, chunkX, chunkZ);
            dist.populate(world, chunkX, chunkZ);
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

    public static void onPopulateChunk(World world, int chunkX, int chunkZ, Random rand) {
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
		return world.getChunk(chunkX, chunkZ).getStatus().isAtLeast(ChunkStatus.FEATURES);
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
                if (currentServer.getWorlds()== null)
                {
                    return false;
                }

                for (ServerWorld world : currentServer.getWorlds()) {
                    if (world != null)
                    {
                        worldInfo = world.getWorldInfo();
                    }

                    if (worldInfo != null)
                    {
                        break;
                    }                	
                }
                
            }

            if (worldInfo == null)
            {
                return false;
            }

            onServerChanged(currentServer, worldInfo);
            return true;
        }
    }

    public static void onServerChanged(MinecraftServer server, WorldInfo worldInfo)
    {
        _worldConfigs.clear();
        WorldConfig.loadedOptionOverrides[1] = WorldConfig.loadedOptionOverrides[2] = null;

        _server = server;
        CustomOreGenBase.log.debug("Server world changed to " + worldInfo.getWorldName());
        File f = new File(_server.getFolderName());
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
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void addOptionsButtonToGui(CreateWorldScreen gui, List<Widget> buttonList)
    {
        if (gui == null)
        {
            _optionsGuiButton = null;
        }
        else
        {
            GuiCustomOreGenSettings settingsGui = new GuiCustomOreGenSettings(gui);
            _optionsGuiButton = new Button(150, 100, 150, 20, "Custom Ore Generation...", (button) -> {
            	gui.getMinecraft().displayGuiScreen(settingsGui);
            });

            Button button1 = _optionsGuiButton;
            
            if (!buttonList.contains(button1))
            {
                button1.x = (gui.width - button1.getWidth()) / 2;
                button1.y = 165;
                buttonList.add(button1);
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void updateOptionsButtonVisibility(CreateWorldScreen gui) {
    	_optionsGuiButton.visible = !(Boolean)ObfuscationReflectionHelper.getPrivateValue(CreateWorldScreen.class, gui, "inMoreWorldOptionsDisplay");
    }
    
	public static void chunkForced(World world, ChunkPos location) {
		if (forcingChunk) { // prevent infinite recursion when there are multiple chunk loaders
			return;
		}
		forcingChunk = true;
		
		WorldConfig cfg = getWorldConfig(world);
		int radius = (cfg.deferredPopulationRange + 15) / 16;
        
        for (int cX = location.x - radius; cX <= location.x + radius; ++cX)
        {
            for (int cZ = location.z - radius; cZ <= location.z + radius; ++cZ)
            {
            	if (cX != location.x && cZ != location.z) {
            		world.getChunk(cX, cZ);
            	}
            }
        }
        
        forcingChunk = false;
	}
}
