package CustomOreGen.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import net.minecraft.src.ModLoader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.xml.sax.SAXException;

import cpw.mods.fml.common.Loader;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.ForgeInterface;
import CustomOreGen.MystcraftInterface;
import CustomOreGen.MystcraftSymbolData;
import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Config.PropertyIO;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.CIStringMap;
import CustomOreGen.Util.MapCollection;

public class WorldConfig
{
    public static Collection<ConfigOption>[] loadedOptionOverrides = new Collection[3];
    public final World world;
    public final WorldInfo worldInfo;
    public final File globalConfigDir;
    public final File worldBaseDir;
    public final File dimensionDir;
    public int deferredPopulationRange;
    public boolean debuggingMode;
    public boolean vanillaOreGen;
    private Collection<IOreDistribution> oreDistributions;
    private Map<String,ConfigOption> configOptions;
    private Map loadedOptions;
    private Map<String,Integer> worldProperties;
    private Map cogSymbolData;
	private Collection<BiomeDescriptor> biomeSets;

    public static WorldConfig createEmptyConfig()
    {
        try
        {
            return new WorldConfig((File)null, (WorldInfo)null, (File)null, (World)null, (File)null);
        }
        catch (Exception var1)
        {
            throw new RuntimeException(var1);
        }
    }

    public WorldConfig() throws IOException, ParserConfigurationException, SAXException
    {
        this(Loader.instance().getConfigDir(), (WorldInfo)null, (File)null, (World)null, (File)null);
    }

    public WorldConfig(WorldInfo worldInfo, File worldBaseDir) throws IOException, ParserConfigurationException, SAXException
    {
        this(Loader.instance().getConfigDir(), worldInfo, worldBaseDir, (World)null, (File)null);
    }

    public WorldConfig(World world) throws IOException, ParserConfigurationException, SAXException
    {
        this(Loader.instance().getConfigDir(), (WorldInfo)null, (File)null, world, (File)null);
    }

    private WorldConfig(File globalConfigDir, WorldInfo worldInfo, File worldBaseDir, World world, File dimensionDir) throws IOException, ParserConfigurationException, SAXException
    {
        this.deferredPopulationRange = 0;
        this.debuggingMode = false;
        this.vanillaOreGen = false;
        this.oreDistributions = new LinkedList();
        this.configOptions = new CIStringMap(new LinkedHashMap());
        this.loadedOptions = new CIStringMap(new LinkedHashMap());
        this.worldProperties = new CIStringMap(new LinkedHashMap());
        this.cogSymbolData = new CIStringMap(new LinkedHashMap());
        this.biomeSets = new LinkedList();
        String configFile;

        if (world != null)
        {
            if (world.getSaveHandler() != null && world.getSaveHandler() instanceof SaveHandler)
            {
                worldBaseDir = (File)ModLoader.getPrivateValue(SaveHandler.class, (SaveHandler)world.getSaveHandler(), 1);
            }
            else
            {
                worldBaseDir = null;
            }

            configFile = world.provider.dimensionId == 0 ? null : "DIM" + world.provider.dimensionId;

            if (CustomOreGenBase.hasForge())
            {
                configFile = ForgeInterface.getWorldDimensionFolder(world);
            }

            if (configFile == null)
            {
                dimensionDir = worldBaseDir;
            }
            else if (worldBaseDir == null)
            {
                dimensionDir = new File(configFile);
            }
            else
            {
                dimensionDir = new File(worldBaseDir, configFile);
            }

            worldInfo = world.getWorldInfo();
        }

        this.world = world;
        this.worldInfo = worldInfo;
        populateWorldProperties(this.worldProperties, world, worldInfo);
        this.worldBaseDir = worldBaseDir;
        this.dimensionDir = dimensionDir;
        this.globalConfigDir = globalConfigDir;

        if (dimensionDir != null)
        {
            CustomOreGenBase.log.finer("Loading config data for dimension \'" + dimensionDir + "\' ...");
        }
        else if (worldBaseDir != null)
        {
            CustomOreGenBase.log.finer("Loading config data for world \'" + worldBaseDir + "\' ...");
        }
        else
        {
            if (globalConfigDir == null)
            {
                return;
            }

            CustomOreGenBase.log.finer("Loading global config \'" + globalConfigDir + "\' ...");
        }

        configFile = null;
        File[] configFileList = new File[3];
        int configFileDepth = this.buildFileList("CustomOreGen_Config.xml", configFileList);

        if (configFileDepth < 0)
        {
            if (dimensionDir != null)
            {
                CustomOreGenBase.log.warning("No config file found for dimension \'" + dimensionDir + "\' at any scope!");
            }
            else if (worldBaseDir != null)
            {
                CustomOreGenBase.log.finer("No config file found for world \'" + worldBaseDir + "\' at any scope.");
            }
            else
            {
                CustomOreGenBase.log.finer("No global config file found.");
            }
        }
        else
        {
            File var16 = configFileList[configFileDepth];
            File[] optionsFileList = new File[3];
            int optionsFileDepth = this.buildFileList("CustomOreGen_Options.txt", optionsFileList);
            File optionsFile = optionsFileList[Math.max(Math.max(1, configFileDepth), optionsFileDepth)];
            ConfigOption vangen;

            for (int defpopOption = configFileDepth; defpopOption < optionsFileList.length; ++defpopOption)
            {
                if (optionsFileList[defpopOption] != null && optionsFileList[defpopOption].exists())
                {
                    PropertyIO.load(this.loadedOptions, new FileInputStream(optionsFileList[defpopOption]));
                }

                if (loadedOptionOverrides[defpopOption] != null)
                {
                    Iterator dbgmd = loadedOptionOverrides[defpopOption].iterator();

                    while (dbgmd.hasNext())
                    {
                        vangen = (ConfigOption)dbgmd.next();

                        if (vangen.getValue() != null)
                        {
                            this.loadedOptions.put(vangen.getName(), vangen.getValue().toString());
                        }
                    }
                }
            }

            (new ConfigParser(this)).parseFile(var16);
            ConfigOption var20;

            if (optionsFile != null && !optionsFile.exists())
            {
            	for (ConfigOption option : this.configOptions.values()) {
            		if (option.getValue() != null) {
            			this.loadedOptions.put(option.getName(), option.getValue().toString());
            		}
            	}
                
                optionsFile.createNewFile();
                String var19 = "CustomOreGen @VERSION@ Config Options";
                PropertyIO.save(this.loadedOptions, new FileOutputStream(optionsFile), var19);
            }

            ConfigOption var21 = (ConfigOption)this.configOptions.get("deferredPopulationRange");

            if (var21 != null && var21 instanceof NumericOption)
            {
                Double var18 = (Double)var21.getValue();
                this.deferredPopulationRange = var18 != null && var18.doubleValue() > 0.0D ? (int)Math.ceil(var18.doubleValue()) : 0;
            }
            else
            {
                CustomOreGenBase.log.warning("Numeric Option \'" + var21 + "\' not found in config file - defaulting to \'" + this.deferredPopulationRange + "\'.");
            }

            var20 = (ConfigOption)this.configOptions.get("debugMode");

            if (var20 != null && var20 instanceof ChoiceOption)
            {
                String var22 = (String)var20.getValue();
                this.debuggingMode = var22 == null ? false : var22.equalsIgnoreCase("on") || var22.equalsIgnoreCase("true");
            }
            else
            {
                CustomOreGenBase.log.warning("Choice Option \'" + var20 + "\' not found in config file - defaulting to \'" + this.debuggingMode + "\'.");
            }

            vangen = (ConfigOption)this.configOptions.get("vanillaOreGen");

            if (vangen != null && vangen instanceof ChoiceOption)
            {
                String value = (String)vangen.getValue();
                this.vanillaOreGen = value == null ? false : value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
            }
            else
            {
                CustomOreGenBase.log.warning("Choice Option \'" + vangen + "\' not found in config file - defaulting to \'" + this.vanillaOreGen + "\'.");
            }
        }
    }

    private int buildFileList(String fileName, File[] files)
    {
        if (files == null)
        {
            files = new File[3];
        }

        if (this.globalConfigDir != null)
        {
            files[0] = new File(this.globalConfigDir, fileName);
        }

        if (this.worldBaseDir != null)
        {
            files[1] = new File(this.worldBaseDir, fileName);
        }

        if (this.dimensionDir != null)
        {
            files[2] = new File(this.dimensionDir, fileName);
        }

        for (int i = files.length - 1; i >= 0; --i)
        {
            if (files[i] != null && files[i].exists())
            {
                return i;
            }
        }

        return -1;
    }

    private void loadOptions(File optionsFile, boolean createIfMissing) throws IOException
    {
        if (optionsFile != null)
        {
            Properties savedOptions = new Properties();

            if (optionsFile.exists())
            {
                savedOptions.load(new FileInputStream(optionsFile));
                this.loadedOptions.putAll(savedOptions);
            }
            else if (createIfMissing)
            {
                optionsFile.createNewFile();
                savedOptions.putAll(this.loadedOptions);
                String headerComment = "CustomOreGen [1.4.6]v2 Config Options";
                savedOptions.store(new FileOutputStream(optionsFile), headerComment);
            }
        }
    }

    private static void populateWorldProperties(Map properties, World world, WorldInfo worldInfo)
    {
        properties.put("world", worldInfo == null ? "" : worldInfo.getWorldName());
        properties.put("world.seed", Long.valueOf(worldInfo == null ? 0L : worldInfo.getSeed()));
        properties.put("world.version", Integer.valueOf(worldInfo == null ? 0 : worldInfo.getSaveVersion()));
        properties.put("world.isHardcore", Boolean.valueOf(worldInfo == null ? false : worldInfo.isHardcoreModeEnabled()));
        properties.put("world.hasFeatures", Boolean.valueOf(worldInfo == null ? false : worldInfo.isMapFeaturesEnabled()));
        properties.put("world.hasCheats", Boolean.valueOf(worldInfo == null ? false : worldInfo.areCommandsAllowed()));
        properties.put("world.gameMode", worldInfo == null ? "" : worldInfo.getGameType().getName());
        properties.put("world.gameMode.id", Integer.valueOf(worldInfo == null ? 0 : worldInfo.getGameType().getID()));
        properties.put("world.type", worldInfo == null ? "" : worldInfo.getTerrainType().getWorldTypeName());
        properties.put("world.type.version", Integer.valueOf(worldInfo == null ? 0 : worldInfo.getTerrainType().getGeneratorVersion()));
        String genName = "RandomLevelSource";
        String genClass = "ChunkProviderGenerate";

        if (world != null)
        {
            IChunkProvider chunkProvider = world.provider.createChunkGenerator();
            genName = chunkProvider.makeString();
            genClass = chunkProvider.getClass().getSimpleName();

            if (chunkProvider instanceof ChunkProviderGenerate)
            {
                genClass = "ChunkProviderGenerate";
            }
            else if (chunkProvider instanceof ChunkProviderFlat)
            {
                genClass = "ChunkProviderFlat";
            }
            else if (chunkProvider instanceof ChunkProviderHell)
            {
                genClass = "ChunkProviderHell";
            }
            else if (chunkProvider instanceof ChunkProviderEnd)
            {
                genName = "EndRandomLevelSource";
                genClass = "ChunkProviderEnd";
            }
        }

        properties.put("dimension.generator", genName);
        properties.put("dimension.generator.class", genClass);
        properties.put("dimension", world == null ? "" : world.provider.getDimensionName());
        properties.put("dimension.id", Integer.valueOf(world == null ? 0 : world.provider.dimensionId));
        properties.put("dimension.isSurface", Boolean.valueOf(world == null ? false : world.provider.isSurfaceWorld()));
        properties.put("dimension.groundLevel", Integer.valueOf(world == null ? 0 : world.provider.getAverageGroundLevel()));
        properties.put("dimension.height", Integer.valueOf(world == null ? 0 : world.getHeight()));
        properties.put("age", Boolean.FALSE);

        if (CustomOreGenBase.hasMystcraft())
        {
            MystcraftInterface.populateAgePropertyMap(world, properties);
        }
    }

    public Collection<IOreDistribution> getOreDistributions()
    {
        return this.oreDistributions;
    }

    public Collection<IOreDistribution> getOreDistributions(String namePattern)
    {
        LinkedList<IOreDistribution> matches = new LinkedList();

        if (namePattern != null)
        {
            Pattern pattern = Pattern.compile(namePattern, 2);
            Matcher matcher = pattern.matcher("");
            for (IOreDistribution dist : this.oreDistributions) {
            	matcher.reset(dist.toString());

                if (matcher.matches())
                {
                    matches.add(dist);
                }
            }
        }

        return Collections.unmodifiableCollection(matches);
    }

    public ConfigOption getConfigOption(String optionName)
    {
        return this.configOptions.get(optionName);
    }

    public Collection<ConfigOption> getConfigOptions()
    {
    	return new MapCollection<String,ConfigOption>(this.configOptions) {
     	    protected String getKey(ConfigOption v)
    	    {
    	        return v.getName();
    	    }
    	};
    }

    public Collection<ConfigOption> getConfigOptions(String namePattern)
    {
        LinkedList<ConfigOption> matches = new LinkedList();

        if (namePattern != null)
        {
            Pattern pattern = Pattern.compile(namePattern, 2);
            Matcher matcher = pattern.matcher("");
            for (ConfigOption option : this.configOptions.values()) {
            	matcher.reset(option.getName());

                if (matcher.matches())
                {
                    matches.add(option);
                }
            }
        }

        return Collections.unmodifiableCollection(matches);
    }

    public String loadConfigOption(String optionName)
    {
        return (String)this.loadedOptions.get(optionName);
    }

    public Object getWorldProperty(String propertyName)
    {
        return this.worldProperties.get(propertyName);
    }

    public MystcraftSymbolData getMystcraftSymbol(String symbolName)
    {
        return (MystcraftSymbolData)this.cogSymbolData.get(symbolName);
    }

    public Collection getMystcraftSymbols()
    {
        return new MapCollection<String,MystcraftSymbolData>(this.cogSymbolData) {
        	protected String getKey(MystcraftSymbolData v)
        	{
        		return v.symbolName;
        	}

        	public boolean add(MystcraftSymbolData v)
        	{
        		String key = "age." + v.symbolName;
        		Integer count = worldProperties.get(key);

        		if (count == null)
        		{
        			worldProperties.put("age." + v.symbolName, 0);
        		}
        		else
        		{
        			v.count = count.intValue();
        		}

        		return super.add(v);
        	}

        };
    }

    public Collection<BiomeDescriptor> getBiomeSets() {
    	return biomeSets;
    }
    
	public Collection<BiomeDescriptor> getBiomeSets(String namePattern) {
        LinkedList<BiomeDescriptor> matches = new LinkedList();

        if (namePattern != null)
        {
            Pattern pattern = Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher("");
            for (BiomeDescriptor desc : this.biomeSets) {
            	matcher.reset(desc.getName());

                if (matcher.matches())
                {
                    matches.add(desc);
                }
            }
        }

        return Collections.unmodifiableCollection(matches);
	}

}
