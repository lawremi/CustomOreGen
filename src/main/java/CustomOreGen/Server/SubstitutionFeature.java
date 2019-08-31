package CustomOreGen.Server;

import java.util.Map;
import java.util.Random;

import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockArrangement;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.TileEntityHelper;
import CustomOreGen.Util.TouchingDescriptorList;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.IFeatureConfig;

/*
 * TODO:
 * 1. Create OreDistributionConfig that extends FeatureConfig and uses @DistributionSetting to handle serialization
 * 2. Move each distribution's settings to a corresponding extension of OreDistributionConfig
 * 3. Move the setting accessors from IOreDstribution to OreDistributionConfig. Will require a lot of refactoring. 
 * 4. Refactor each distribution's generate() into place(), i.e., based on the config
 */
public class SubstitutionFeature extends Feature<SubstitutionFeatureConfig> implements IOreDistribution
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
        this.seed = (new Random((long)distributionID)).nextLong();
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

    public void generate(World world, int chunkX, int chunkZ) {}

    public void populate(World world, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid && this.oreBlock != null)
        {
            Random random = new Random(world.getSeed());
            long xSeed = random.nextLong() >> 3;
            long zSeed = random.nextLong() >> 3;
            random.setSeed(xSeed * (long)chunkX + zSeed * (long)chunkZ ^ world.getSeed() ^ this.seed);
            this.generate(world, random, new BlockPos(chunkX * 16, 0, chunkZ * 16));
        }
    }

    public void cull() {}

    public void clear()
    {
        this.populatedChunks = 0;
        this.placedBlocks = 0L;
    }

    public GeometryStream getDebuggingGeometry(World world, int chunkX, int chunkZ)
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
	public boolean place(IWorld worldIn, ChunkGenerator<? extends GenerationSettings> generator, Random rand, BlockPos pos, IFeatureConfig config) {
		// TODO Auto-generated method stub
		return false;
	}

    public boolean generate(World world, Random random, BlockPos position)
    {
        if (this._canGenerate && this._valid && this.oreBlock != null)
        {
        	int depositCX = position.getX() / 16;
            int depositCZ = position.getZ() / 16;
            int cRange = (this.additionalRange + 15) / 16;
            int hRange = (this.additionalRange + 7) / 8;
            int minh = Math.max(0, this.minHeight);
            int maxh = Math.min(world.getHeight() - 1, this.maxHeight);
            BlockArrangement arrangement = new BlockArrangement(replaceableBlocks, aboveBlocks, belowBlocks,
                    besideBlocks, touchingBlocks);

            for (int dCX = -cRange; dCX <= cRange; ++dCX)
            {
                for (int dCZ = -cRange; dCZ <= cRange; ++dCZ)
                {
                	int chunkZ = depositCZ + dCZ;
                    int chunkX = depositCX + dCX;

                    Chunk chunk = (Chunk)world.getChunk(chunkX, chunkZ, ChunkStatus.FEATURES.getParent(), false);
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
                            	Biome biome = chunk.getBiome(new BlockPos(x, 0, z));

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
                                            if (match != null && world.setBlockState(new BlockPos(worldX, y, worldZ), match.getBlockState(), 2))
                                            {
                                                ++this.placedBlocks;
                                                TileEntityHelper.readFromPartialNBT(world, worldX, y, worldZ, match.getNBT());
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
    
    private int findSurfaceHeight(Chunk chunk, int x, int z) {
    	return chunk.getTopBlockY(Heightmap.Type.WORLD_SURFACE, x, z);
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
