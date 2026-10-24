package CustomOreGen.Server;

import java.util.Map;

import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockArrangement;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import CustomOreGen.Util.BlockEntityHelper;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.TouchingDescriptorList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/*
 * TODO:
 * 1. Create OreDistributionConfig that extends FeatureConfig and uses @DistributionSetting to handle serialization
 * 2. Move each distribution's settings to a corresponding extension of OreDistributionConfig
 * 3. Move the setting accessors from IOreDstribution to OreDistributionConfig. Will require a lot of refactoring. 
 * 4. Refactor each distribution's generate() into place(), i.e., based on the config
 */
public class SubstitutionFeature extends Feature<NoneFeatureConfiguration> implements IOreDistribution
{
    @DistributionSetting(
            name = "Name",
            inherited = false,
            info = "Descriptive distribution name."
    )
    public String name;
    
    @DistributionSetting(
            name = "DisplayName",
            inherited = false,
            info = "Distribution name for display in user interfaces."
    )
    public String displayName;
    
    @DistributionSetting(
            name = "Seed",
            inherited = false,
            info = "Distribution random number seed."
    )
    public long seed;
    
    @DistributionSetting(
            name = "OreBlock",
            info = "Ore block(s) - total weight must not be more than 100%"
    )
    public final BlockDescriptor oreBlock;
    
    @DistributionSetting(
            name = "Replaces",
            info = "List of replaceable blocks"
    )
    public final BlockDescriptor replaceableBlocks;
    
    @DistributionSetting(
            name = "PlacesAbove",
            info = "List of blocks allowed below generated block"
    )
    public final BlockDescriptor belowBlocks;
    
    @DistributionSetting(
            name = "PlacesBelow",
            info = "List of blocks allowed above generated block"
    )
    public final BlockDescriptor aboveBlocks;
    
    @DistributionSetting(
            name = "PlacesBeside",
            info = "List of blocks allowed beside generated block"
    )
    public final BlockDescriptor besideBlocks;

    @DistributionSetting(
            name = "Touches",
            info = "List of blocks allowed to neighbor the generated block"
    )
    public final TouchingDescriptorList touchingBlocks;

    @DistributionSetting(
            name = "TargetBiome",
            info = "List of valid target biomes"
    )
    public final BiomeDescriptor biomes;
    
    @DistributionSetting(
            name = "additionalRange",
            info = "Distance outside of current chunk to scan in every pass, in meters"
    )
    public int additionalRange;
    
    @DistributionSetting(
            name = "minHeight",
            info = "Minimum substitution height"
    )
    public int minHeight;
    
    @DistributionSetting(
            name = "maxHeight",
            info = "Maximum substitution height"
    )
    public int maxHeight;
    
    @DistributionSetting(
            name = "minSurfRelHeight",
            info = "Minimum surface-relative substitution height"
    )
    public int minSurfRelHeight;
    
    @DistributionSetting(
            name = "maxSurfRelHeight",
            info = "Maximum surface-relative substitution height"
    )
    public int maxSurfRelHeight;
    
    @DistributionSetting(
            name = "populatedChunks",
            info = "Chunks populated during current game session."
    )
    public int populatedChunks;
    
    @DistributionSetting(
            name = "placedBlocks",
            info = "Blocks placed during current game session."
    )
    public long placedBlocks;
    
    protected boolean _valid;
    protected final boolean _canGenerate;
    protected static final DistributionSettingMap settingMap = new DistributionSettingMap(SubstitutionFeature.class);

    public SubstitutionFeature(int distributionID, boolean canGenerate)
    {
    	super(NoneFeatureConfiguration.CODEC);
        this.oreBlock = new BlockDescriptor(Blocks.STONE);
        this.replaceableBlocks = new BlockDescriptor();
        this.aboveBlocks = new BlockDescriptor();
        this.belowBlocks = new BlockDescriptor();
        this.besideBlocks = new BlockDescriptor();
        this.touchingBlocks = new TouchingDescriptorList();
        this.biomes = new BiomeDescriptor(".*");
        this.additionalRange = 0;
        this.minHeight = Integer.MIN_VALUE;
        this.maxHeight = Integer.MAX_VALUE;
        this.minSurfRelHeight = Integer.MIN_VALUE;
        this.maxSurfRelHeight = Integer.MAX_VALUE;
        this.populatedChunks = 0;
        this.placedBlocks = 0L;
        this._valid = false;
        this.name = "Substitute_" + distributionID;
        this.seed = (RandomSource.create((long)distributionID)).nextLong();
        this._canGenerate = canGenerate;
    }

    public void inheritFrom(IOreDistribution inherits) throws IllegalArgumentException
    {
        if (inherits != null && inherits instanceof SubstitutionFeature)
        {
            settingMap.inheritAll((SubstitutionFeature)inherits, this);
            this._valid = false;
        }
        else
        {
            throw new IllegalArgumentException("Invalid source distribution \'" + inherits + "\'");
        }
    }

    public Map<String,String> getDistributionSettingDescriptions()
    {
        return settingMap.getDescriptions();
    }

    public Object getDistributionSetting(String settingName)
    {
        return settingMap.get(this, settingName);
    }

    public void setDistributionSetting(String settingName, Object value) throws IllegalArgumentException, IllegalAccessException
    {
        settingMap.set(this, settingName, value);
    }

    //public void generate(Level world, int chunkX, int chunkZ) {}

	@Override
    public void populate(WorldGenLevel world, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid && this.oreBlock != null)
        {
        	long seed = world.getServer().getWorldData().worldGenOptions().seed();
            RandomSource random = RandomSource.create(seed);
            long xSeed = random.nextLong() >> 3;
            long zSeed = random.nextLong() >> 3;
            random.setSeed(xSeed * (long)chunkX + zSeed * (long)chunkZ ^ seed ^ this.seed);
            this.generate(world, random, new BlockPos(chunkX * 16, 0, chunkZ * 16));
        }
    }

    public void cull() {}

    public void clear()
    {
        this.populatedChunks = 0;
        this.placedBlocks = 0L;
    }

    public GeometryStream getDebuggingGeometry(LevelAccessor world, int chunkX, int chunkZ)
    {
        return null;
    }

    public boolean validate() throws IllegalStateException
    {
        this._valid = true;
        float oreBlockMatchWeight = this.oreBlock.getTotalMatchWeight();

        if (oreBlockMatchWeight <= 0.0F)
        {
            if (this._canGenerate)
            {
                this._valid = false;
                throw new IllegalStateException("Ore block descriptor for " + this + " is empty or does not match any registered blocks.");
            }
        }
        else if (oreBlockMatchWeight > 1.0F)
        {
            this._valid = false;
            throw new IllegalStateException("Ore block descriptor for " + this + " is overspecified with a total match weight of " + oreBlockMatchWeight * 100.0F + "%.");
        }

        float replBlockMatchWeight = this.replaceableBlocks.getTotalMatchWeight();

        if (replBlockMatchWeight <= 0.0F)
        {
            ;
        }

        float biomeMatchWeight = this.biomes.getTotalMatchWeight();

        if (biomeMatchWeight <= 0.0F)
        {
            ;
        }

        if (this.additionalRange < 0)
        {
            this._valid = false;
            throw new IllegalStateException("Invalid additional scan range \'" + this.additionalRange + "\' for " + this);
        }
        else if (this.minHeight > this.maxHeight)
        {
            this._valid = false;
            throw new IllegalStateException("Invalid height range [" + this.minHeight + "," + this.maxHeight + "] for " + this);
        }
        else if (this.minSurfRelHeight > this.maxSurfRelHeight)
        {
            this._valid = false;
            throw new IllegalStateException("Invalid height range [" + this.minSurfRelHeight + "," + this.maxSurfRelHeight + "] for " + this);
        }
        else
        {
            return this._valid && this._canGenerate;
        }
    }
	
	@Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return generate(context.level(), context.random(), context.origin());
    }

	// Substitutions have no layout to precompute; all the work happens in populate().
	@Override
	public void generate(LevelAccessor world, int chunkX, int chunkZ) {}

    public boolean generate(LevelAccessor world, RandomSource random, BlockPos position)
    {
        if (this._canGenerate && this._valid && this.oreBlock != null)
        {
        	int depositCX = position.getX() >> 4;
            int depositCZ = position.getZ() >> 4;
            // additionalRange was how pre-1.13 COG reached ore spilled in from neighbors. Population is now
            // deferred until no neighbor can write here (ServerState.onChunkFeaturesDone), so only this
            // chunk is scanned; writing into neighbors would also leave the generation region's write area.
            int cRange = 0;
            int hRange = (this.additionalRange + 7) / 8;
            int minh = Math.max(world.getMinBuildHeight(), this.minHeight);
            int maxh = Math.min(world.getMaxBuildHeight() - 1, this.maxHeight);
            BlockArrangement arrangement = new BlockArrangement(replaceableBlocks, aboveBlocks, belowBlocks,
                    besideBlocks, touchingBlocks);

            for (int dCX = -cRange; dCX <= cRange; ++dCX)
            {
                for (int dCZ = -cRange; dCZ <= cRange; ++dCZ)
                {
                	int chunkZ = depositCZ + dCZ;
                    int chunkX = depositCX + dCX;

                    ChunkAccess chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FEATURES.getParent(), false);
                    if (chunk != null)
                    {
                        int minX = dCX < 0 && -dCX * 2 > hRange ? 8 : 0;
                        int minZ = dCZ < 0 && -dCZ * 2 > hRange ? 8 : 0;
                        int maxX = dCX > 0 && dCX * 2 > hRange ? 8 : 16;
                        int maxZ = dCZ > 0 && dCZ * 2 > hRange ? 8 : 16;

                        for (int x = minX; x < maxX; ++x)
                        {
                            for (int z = minZ; z < maxZ; ++z)
                            {
                            	// Noise biomes are stored per 4x4x4 cell, addressed in quart (block / 4) coordinates.
                            	Biome biome = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(world.getSeaLevel()), QuartPos.fromBlock(z)).get();

                                if (biome == null || this.biomes.getWeight(biome) > 0.5F)
                                {
                                	int xzminh = minh;
                                	int xzmaxh = maxh;
                                	if (this.minSurfRelHeight != Integer.MIN_VALUE || this.maxSurfRelHeight != Integer.MAX_VALUE) {
                                		int surfh = findSurfaceHeight(chunk, x, z);
	                                	xzminh = Math.max(xzminh, this.minSurfRelHeight + surfh);
	                                	xzmaxh = Math.min(xzmaxh, this.maxSurfRelHeight + 
	                                			                  Math.min(surfh, Integer.MAX_VALUE - this.maxSurfRelHeight));
                                	}
                                    for (int y = xzminh; y <= xzmaxh; ++y)
                                    {
                                    	int worldX = chunkX * 16 + x;
                                    	int worldZ = chunkZ * 16 + z;
                                    	BlockPos worldPos = new BlockPos(worldX, y, worldZ);
                                    	if (arrangement.matchesAt(world, random, worldPos)) {	
                                            BlockInfo match = this.oreBlock.getMatchingBlock(random);
                                            if (match == null)
                                            {
                                                return false;
                                            }
                                            if (match != null && world.setBlock(new BlockPos(worldX, y, worldZ), match.getBlockState(), 2))
                                            {
                                                ++this.placedBlocks;
                                                BlockEntityHelper.readFromPartialNBT(world, worldX, y, worldZ, match.getNBT());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ++this.populatedChunks;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    private int findSurfaceHeight(ChunkAccess chunk, int x, int z) {
    	return chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
	}

	public String toString()
    {
        return this.name;
    }

	@Override
	public double getOresPerChunk() {
		return this.maxHeight - this.minHeight;
	}
}
