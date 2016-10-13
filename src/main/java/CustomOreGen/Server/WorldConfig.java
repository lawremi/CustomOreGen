package CustomOreGen.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.Maps;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.ForgeInterface;
import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Config.PropertyIO;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.CIStringMap;
import CustomOreGen.Util.MapCollection;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.gen.ChunkProviderOverworld;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class WorldConfig
{
	@SuppressWarnings("unchecked")
	public static Collection<ConfigOption>[] loadedOptionOverrides = new Collection[3];
    public final World world;
    public final WorldInfo worldInfo;
    public final File globalConfigDir;
    public final File worldBaseDir;
    public final File dimensionDir;
    public int deferredPopulationRange;
    public boolean debuggingMode;
    public boolean vanillaOreGen;
    public boolean custom;
    private Map<String,IOreDistribution> oreDistributions;
    private Map<String,ConfigOption> configOptions;
    private Map<String,String> loadedOptions;
    private Map<String,Object> worldProperties;
    private Map<String,BiomeDescriptor> biomeSets;
	private BlockDescriptor equivalentBlockDescriptor;
	private int idCounter;

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
        this(CustomOreGenBase.getConfigDir(), (WorldInfo)null, (File)null, (World)null, (File)null);
    }

    public WorldConfig(WorldInfo worldInfo, File worldBaseDir) throws IOException, ParserConfigurationException, SAXException
    {
        this(CustomOreGenBase.getConfigDir(), worldInfo, worldBaseDir, (World)null, (File)null);
    }

    public WorldConfig(World world) throws IOException, ParserConfigurationException, SAXException
    {
        this(CustomOreGenBase.getConfigDir(), (WorldInfo)null, (File)null, world, (File)null);
    }

    private WorldConfig(File globalConfigDir, WorldInfo worldInfo, File worldBaseDir, World world, File dimensionDir) throws IOException, ParserConfigurationException, SAXException
    {
        this.deferredPopulationRange = 0;
        this.debuggingMode = false;
        this.vanillaOreGen = false;
        this.oreDistributions = new LinkedHashMap<String,IOreDistribution>();
        this.configOptions = new CIStringMap<ConfigOption>(new LinkedHashMap<String, ConfigOption>());
        this.loadedOptions = new CIStringMap<String>(new LinkedHashMap<String, String>());
        this.worldProperties = new CIStringMap<Object>(new LinkedHashMap<String, Object>());
        this.biomeSets = new CIStringMap<BiomeDescriptor>();
        String dimensionBasename;

        if (world != null)
        {
            if (world.getSaveHandler() != null && world.getSaveHandler() instanceof SaveHandler)
            {
                worldBaseDir = ((SaveHandler)world.getSaveHandler()).getWorldDirectory();
            }
            else
            {
                worldBaseDir = null;
            }

            dimensionBasename = "DIM" + world.provider.getDimension();

            if (world.provider.getDimension() != 0)
            {
            	dimensionBasename = ForgeInterface.getWorldDimensionFolder(world);
            }

            if (worldBaseDir == null)	
            {
                dimensionDir = new File(dimensionBasename);
            }
            else
            {
                dimensionDir = new File(worldBaseDir, dimensionBasename);
            }

            worldInfo = world.getWorldInfo();
        }

        if (dimensionDir == null && worldBaseDir != null) {
        	dimensionDir = new File(worldBaseDir, "DIM0");
            if (!dimensionDir.exists())
            	dimensionDir.mkdir();
        }
        
        this.world = world;
        this.worldInfo = worldInfo;
        populateWorldProperties(this.worldProperties, world, worldInfo);
        this.worldBaseDir = worldBaseDir;
        this.dimensionDir = dimensionDir;
        this.globalConfigDir = globalConfigDir;

        if (dimensionDir != null)
        {
            CustomOreGenBase.log.info("Loading config data for dimension \'" + dimensionDir + "\' ...");
        }
        else
        {
            if (globalConfigDir == null)
            {
                return;
            }

            CustomOreGenBase.log.info("Loading global config \'" + globalConfigDir + "\' ...");
        }

        File configFile = null;
        File[] configFileList = new File[3];
        int configFileDepth = this.buildFileList(CustomOreGenBase.BASE_CONFIG_FILENAME, configFileList, true);

        if (configFileDepth >= 0) 
        {
        	configFile = configFileList[configFileDepth];
        } 
        else 
        {
        	File defaultConfigFile = new File(globalConfigDir, CustomOreGenBase.DEFAULT_BASE_CONFIG_FILENAME);
        	if (defaultConfigFile.exists()) 
        	{
        		configFile = defaultConfigFile;
        		configFileDepth = 0;
        	}
        	else 
        	{
        		if (dimensionDir != null)
        		{
        			CustomOreGenBase.log.warn("No config file found for dimension \'" + dimensionDir + "\' at any scope!");
        		}
        		else
        		{
        			CustomOreGenBase.log.info("No global config file found.");
        		}
        	}
        }
        
        if (configFile != null) {
            File[] optionsFileList = new File[3];
            this.buildFileList(CustomOreGenBase.OPTIONS_FILENAME, optionsFileList, false);
            File optionsFile = optionsFileList[2];
            ConfigOption vangen;

            for (int defpopOption = configFileDepth; defpopOption < optionsFileList.length; ++defpopOption)
            {
                loadOptions(optionsFileList[defpopOption], loadedOptionOverrides[defpopOption], this.loadedOptions);
            }

            (new ConfigParser(this)).parseFile(configFile);
            ConfigOption option;

            Map<String,String> saveLevelOptions = new LinkedHashMap<String, String>();
            if (optionsFileList[1] != null) {
            	if (!optionsFileList[1].exists()) {
            		loadOptions(optionsFileList[0], loadedOptionOverrides[0], saveLevelOptions);
            		saveOptions(optionsFileList[1], saveLevelOptions);
            		loadedOptionOverrides[0] = null;
            	} else {
            		loadOptions(optionsFileList[1], null, saveLevelOptions);
            	}
            }
            
            if (optionsFile != null)
            {
            	putOptions(this.configOptions.values(), this.loadedOptions);
            	if (world != null) {
            		Map<String,String> dimLevelOptions = setdiffOptions(this.loadedOptions, saveLevelOptions);
            		loadOptions(optionsFile, loadedOptionOverrides[2], dimLevelOptions);
            		saveOptions(optionsFile, dimLevelOptions);
            	}
            }

            option = (ConfigOption)this.configOptions.get("deferredPopulationRange");

            if (option != null && option instanceof NumericOption)
            {
                Double var18 = (Double)option.getValue();
                this.deferredPopulationRange = var18 != null && var18.doubleValue() > 0.0D ? (int)Math.ceil(var18.doubleValue()) : 0;
            }
            else
            {
                CustomOreGenBase.log.warn("Numeric Option \'" + option + "\' not found in config file - defaulting to \'" + this.deferredPopulationRange + "\'.");
            }

            option = (ConfigOption)this.configOptions.get("debugMode");

            if (option != null && option instanceof ChoiceOption)
            {
                String debugValue = (String)option.getValue();
                this.debuggingMode = debugValue == null ? false : debugValue.equalsIgnoreCase("on") || debugValue.equalsIgnoreCase("true");
            }
            else
            {
                CustomOreGenBase.log.warn("Choice Option \'" + option + "\' not found in config file - defaulting to \'" + this.debuggingMode + "\'.");
            }

            vangen = (ConfigOption)this.configOptions.get("vanillaOreGen");

            if (vangen != null && vangen instanceof ChoiceOption)
            {
                String value = (String)vangen.getValue();
                this.vanillaOreGen = value == null ? false : value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
            }
            else
            {
                CustomOreGenBase.log.warn("Choice Option \'" + vangen + "\' not found in config file - defaulting to \'" + this.vanillaOreGen + "\'.");
            }
            
        }
    }

	private Map<String, String> setdiffOptions(Map<String, String> x, Map<String, String> y) {
		return new LinkedHashMap<String, String>(Maps.difference(x, y).entriesOnlyOnLeft());
	}

	private void loadOptions(File file, Collection<ConfigOption> overrides, Map<String, String> map) throws FileNotFoundException, IOException {
		if (file != null && file.exists())
        {
            PropertyIO.load(map, new FileInputStream(file));
        }

        if (overrides != null)
        {
        	putOptions(overrides, map);
        }
	}

	private void putOptions(Collection<ConfigOption> options, Map<String, String> map) {
    	for (ConfigOption option : options) {
    		if (option.getValue() != null) {
    			map.put(option.getName(), option.getValue().toString());
    		}
    	}
	}

	private void saveOptions(File optionsFile, Map<String, String> options) throws IOException {
    	optionsFile.createNewFile();
        String header = CustomOreGenBase.getDisplayString() + " Config Options";
        PropertyIO.save(options, new FileOutputStream(optionsFile), header);
	}

	private int buildFileList(String fileName, File[] files, boolean mustExist)
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

        if (this.dimensionDir != null && dimensionDir.exists())
        {
            files[2] = new File(this.dimensionDir, fileName);
        }

        for (int i = files.length - 1; i >= 0; --i)
        {
            if (files[i] != null && (!mustExist || files[i].exists()))
            {
                return i;
            }
        }

        return -1;
    }

    private static void populateWorldProperties(Map<String,Object> properties, World world, WorldInfo worldInfo)
    {
        properties.put("world", worldInfo == null ? "" : worldInfo.getWorldName());
        properties.put("world.seed", worldInfo == null ? 0L : worldInfo.getSeed());
        properties.put("world.version", worldInfo == null ? 0 : worldInfo.getSaveVersion());
        properties.put("world.isHardcore", worldInfo == null ? false : worldInfo.isHardcoreModeEnabled());
        properties.put("world.hasFeatures", worldInfo == null ? false : worldInfo.isMapFeaturesEnabled());
        properties.put("world.hasCheats", worldInfo == null ? false : worldInfo.areCommandsAllowed());
        properties.put("world.gameMode", worldInfo == null ? "" : worldInfo.getGameType().getName());
        properties.put("world.gameMode.id", worldInfo == null ? 0 : worldInfo.getGameType().getID());
        properties.put("world.type", worldInfo == null ? "" : worldInfo.getTerrainType().getWorldTypeName());
        properties.put("world.type.version", worldInfo == null ? 0 : worldInfo.getTerrainType().getGeneratorVersion());
        String genName = "RandomLevelSource";
        String genClass = "ChunkProviderOverworld";

        if (world != null)
        {
            IChunkGenerator chunkGenerator = world.provider.createChunkGenerator();
            genName = chunkGenerator.toString();
            genClass = chunkGenerator.getClass().getSimpleName();

            if (chunkGenerator instanceof ChunkProviderOverworld)
            {
                genClass = "ChunkProviderOverworld";
            }
            else if (chunkGenerator instanceof ChunkProviderFlat)
            {
                genClass = "ChunkProviderFlat";
            }
            else if (chunkGenerator instanceof ChunkProviderHell)
            {
                genClass = "ChunkProviderHell";
            }
            else if (chunkGenerator instanceof ChunkProviderEnd)
            {
                genName = "EndRandomLevelSource";
                genClass = "ChunkProviderEnd";
            }
        }

        properties.put("dimension.generator", genName);
        properties.put("dimension.generator.class", genClass);
        properties.put("dimension", world == null ? "" : world.provider.getDimensionType().getName());
        properties.put("dimension.id", world == null ? 0 : world.provider.getDimension());
        properties.put("dimension.isSurface", world == null ? false : world.provider.isSurfaceWorld());
        properties.put("dimension.hasNoSky", world == null ? false : world.provider.getHasNoSky());
        properties.put("dimension.groundLevel", world == null ? 0 : world.provider.getAverageGroundLevel());
        properties.put("dimension.actualHeight", world == null ? 0 : world.getActualHeight());
        properties.put("dimension.height", world == null ? 0 : world.getHeight());
        properties.put("age", false);
    }

    public Collection<IOreDistribution> getOreDistributions()
    {
        return this.oreDistributions.values();
    }

    public Collection<IOreDistribution> getOreDistributions(String namePattern)
    {
        LinkedList<IOreDistribution> matches = new LinkedList<IOreDistribution>();

        if (namePattern != null)
        {
            Pattern pattern = Pattern.compile(namePattern, 2);
            Matcher matcher = pattern.matcher("");
            for (IOreDistribution dist : this.oreDistributions.values()) {
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
        LinkedList<ConfigOption> matches = new LinkedList<ConfigOption>();

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
    
	public BiomeDescriptor getBiomeSet(String namePattern) {
        return this.biomeSets.get(namePattern);
	}	

	public BlockDescriptor getEquivalentBlockDescriptor() {
		if (this.equivalentBlockDescriptor == null) {
			this.equivalentBlockDescriptor = this.makeEquivalentBlockDescriptor();
		}
		return this.equivalentBlockDescriptor;
	}

	private BlockDescriptor makeEquivalentBlockDescriptor() {
		double totalWeight = 0;
		for (IOreDistribution dist : this.oreDistributions.values()) {
			totalWeight += dist.getOresPerChunk();
		}
		BlockDescriptor desc = new BlockDescriptor();
		for (IOreDistribution dist : this.oreDistributions.values()) {
			BlockDescriptor oreBlock = (BlockDescriptor)dist.getDistributionSetting("OreBlock");
			desc.add(oreBlock, (float)(dist.getOresPerChunk() / totalWeight));
		}
		return desc;
	}

	public void registerDistribution(String newName, IOreDistribution distribution) {
		if (this.oreDistributions.containsKey(newName)) {
			this.oreDistributions.remove(newName); // otherwise, order is not updated
		}
		this.oreDistributions.put(newName, distribution);
	}

	public void registerBiomeSet(BiomeDescriptor biomeSet) {
		this.biomeSets.put(biomeSet.getName(), biomeSet);
	}
	
	public int nextDistributionID() {
		return this.idCounter++;
	}
}
