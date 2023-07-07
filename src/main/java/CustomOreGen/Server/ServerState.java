package CustomOreGen.Server;

import java.awt.Frame;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.GeometryData;
import CustomOreGen.GeometryRequestData;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.SimpleProfiler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ServerState
{
    private static MinecraftServer _server = null;
    private static Map<LevelAccessor,WorldConfig> _worldConfigs = new HashMap<LevelAccessor, WorldConfig>();
    private static Button _optionsGuiButton = null;
    private static boolean forcingChunk;

    public static WorldConfig getWorldConfig(LevelAccessor world)
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
        	System.out.println(dist.toString());
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

    public static void onPopulateChunk(WorldConfig cfg, LevelAccessor world, int chunkX, int chunkZ, RandomSource rand) {
    	int range = (cfg.deferredPopulationRange + 15) / 16;
    	//for (int iX = chunkX - range; iX <= chunkX + range; ++iX)
        {
            //for (int iZ = chunkZ - range; iZ <= chunkZ + range; ++iZ)
            {
            	//if (allNeighborsPopulated(world, iX, iZ, range))
            	{
            		//CustomOreGenBase.log.info("[" + iX + "," + iZ + "]: POPULATING");
            		populateDistributions(cfg.getOreDistributions(), world, chunkX, chunkZ);
            	}
            }
        }
    }

    @SuppressWarnings("unused")
	private static boolean allNeighborsPopulated(LevelAccessor world, int chunkX, int chunkZ, int range) {
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

	private static boolean chunkHasBeenPopulated(LevelAccessor world, int chunkX, int chunkZ) {
		return world.getChunk(chunkX, chunkZ).getStatus().isOrAfter(ChunkStatus.FEATURES);
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

    @OnlyIn(Dist.CLIENT)
    public static void addOptionsButtonToGui(CreateWorldScreen gui, List<GuiEventListener> buttonList, Consumer<GuiEventListener> add)
    {
        if (gui == null)
        {
            _optionsGuiButton = null;
        }
        else
        {
            GuiCustomOreGenSettings settingsGui = new GuiCustomOreGenSettings(gui);
            
            var action = new Button.OnPress() {
                @Override
                public void onPress(Button b) {
                	gui.getMinecraft().forceSetScreen(settingsGui);
                }
            };
            
            _optionsGuiButton = Button.builder(MutableComponent.create(new LiteralContents("Custom Ore Generation...")), action).build();
            
            Button button1 = _optionsGuiButton;
            
            if (!buttonList.contains(button1))
            {
            	int n = gui.children().size();
            	
            	GuiEventListener b = gui.children().get(n-1);
            	
            	button1.setX(b.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL));
            	button1.setY(b.getRectangle().bottom() + 1);
                add.accept(button1);
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void updateOptionsButtonVisibility(CreateWorldScreen gui) {
    	net.minecraft.client.gui.components.tabs.TabNavigationBar tabBar = (net.minecraft.client.gui.components.tabs.TabNavigationBar)gui.renderables.get(0);
    	int j = 0;
    	for(GuiEventListener child : tabBar.children()) {
    		net.minecraft.client.gui.components.TabButton tb = ((net.minecraft.client.gui.components.TabButton)child);
    		if(tb.isFocused()) {
    			break;
    		}
    		j++;
    	}
    	
    	GuiEventListener b = gui.children().get(0);
    	
    	_optionsGuiButton.setX(b.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL) - _optionsGuiButton.getWidth()/2);
    	
    	b = gui.children().get(gui.children().size() - 1);
    	
    	_optionsGuiButton.setY(b.getRectangle().bottom() + 1);
        
    	//TabNavigationBar bar;
    	//bar.getFocused().getTabOrderGroup()
    	
    	_optionsGuiButton.visible = j == 1;//!(Boolean)ObfuscationReflectionHelper.getPrivateValue(CreateWorldScreen.class, gui, "inMoreLevelOptionsDisplay");
    }
    
	public static void chunkForced(Level world, ChunkPos location) {
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
