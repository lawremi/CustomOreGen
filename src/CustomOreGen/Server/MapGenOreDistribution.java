package CustomOreGen.Server;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.WireframeShapes;

public abstract class MapGenOreDistribution extends MapGenStructure implements IOreDistribution
{
    @DistributionSetting(
            name = "Name",
            inherited = false,
            info = "Descriptive distribution name."
    )
    public String name;
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
    public final BlockDescriptor oreBlock = new BlockDescriptor();
    @DistributionSetting(
            name = "ReplaceableBlock",
            info = "List of replaceable blocks"
    )
    public final BlockDescriptor replaceableBlocks;
    @DistributionSetting(
            name = "TargetBiome",
            info = "List of valid target biomes"
    )
    public final BiomeDescriptor biomes;
    @DistributionSetting(
            name = "DistributionFrequency",
            info = "Number of distribution structures per 16x16 chunk"
    )
    public final PDist frequency;
    @DistributionSetting(
            name = "Parent",
            info = "The parent distribution, or null if no parent"
    )
    public MapGenOreDistribution parent;
    @DistributionSetting(
            name = "ParentRangeLimit",
            info = "Max horizontal distance to a parent distribution, in meters"
    )
    public final PDist parentRangeLimit;
    @DistributionSetting(
            name = "drawBoundBox",
            info = "Whether bounding boxes are drawn for components"
    )
    public boolean wfHasBB;
    @DistributionSetting(
            name = "boundBoxColor",
            info = "Color of bounding boxes for components"
    )
    public long wfBBColor;
    @DistributionSetting(
            name = "drawWireframe",
            info = "Whether wireframes are drawn for components"
    )
    public boolean wfHasWireframe;
    @DistributionSetting(
            name = "wireframeColor",
            info = "Color of wireframes for components"
    )
    public long wfWireframeColor;
    @DistributionSetting(
            name = "completedStructures",
            info = "Structures completed during current game session."
    )
    public int completedStructures;
    @DistributionSetting(
            name = "completedStructureBlocks",
            info = "Blocks placed in structures completed during current game session."
    )
    public long completedStructureBlocks;
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
    protected Map<Long,GeometryStream> debuggingGeometryMap;
    protected boolean _valid;
    protected final boolean _canGenerate;
    private StructureGroup newestGroup;
    private StructureGroup oldestGroup;
    protected final DistributionSettingMap _settingMap;

    public MapGenOreDistribution(DistributionSettingMap settingMap, int distributionID, boolean canGenerate)
    {
        this.replaceableBlocks = new BlockDescriptor(Integer.toString(Block.stone.blockID));
        this.biomes = new BiomeDescriptor(".*");
        this.frequency = new PDist(0.025F, 0.0F);
        this.parent = null;
        this.parentRangeLimit = new PDist(32.0F, 32.0F, Type.normal);
        this.wfHasBB = false;
        this.wfBBColor = -2147483648L;
        this.wfHasWireframe = false;
        this.wfWireframeColor = -15294967L;
        this.completedStructures = 0;
        this.completedStructureBlocks = 0L;
        this.populatedChunks = 0;
        this.placedBlocks = 0L;
        this.debuggingGeometryMap = new HashMap();
        this._valid = false;
        this.newestGroup = null;
        this.oldestGroup = null;
        this.name = "Distribution_" + distributionID;
        this.seed = (new Random((long)distributionID)).nextLong();
        this._canGenerate = canGenerate;
        this._settingMap = settingMap;
    }

    public void inheritFrom(IOreDistribution inherits) throws IllegalArgumentException
    {
        if (inherits != null && this.getClass().isInstance(inherits))
        {
            this._settingMap.inheritAll(inherits, this);
            this._valid = false;
        }
        else
        {
            throw new IllegalArgumentException("Invalid source distribution \'" + inherits + "\'");
        }
    }

    public Map getDistributionSettings()
    {
        return this._settingMap.getDescriptions();
    }

    public Object getDistributionSetting(String settingName)
    {
        return this._settingMap.get(this, settingName);
    }

    public void setDistributionSetting(String settingName, Object value) throws IllegalArgumentException, IllegalAccessException
    {
        this._settingMap.set(this, settingName, value);
    }

    public void generate(World world, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid)
        {
            if (world != super.worldObj)
            {
                this.clear();
            }

            this.generate(world.getChunkProvider(), world, chunkX, chunkZ, (byte[])null);
        }
    }

    public void populate(World world, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid)
        {
            Random random = new Random(world.getSeed());
            long xSeed = random.nextLong() >> 3;
            long zSeed = random.nextLong() >> 3;
            random.setSeed(xSeed * (long)chunkX + zSeed * (long)chunkZ ^ world.getSeed() ^ this.seed);
            this.generateStructuresInChunk(world, random, chunkX, chunkZ);
        }
    }

    public void cull()
    {
        if (this._canGenerate)
        {
            int groupsToSave = (int)(6.0F * Math.min(1.0F, this.frequency.getMax()) * (float)(2 * super.range + 1) * (float)(2 * super.range + 1));

            if (super.structureMap.size() > groupsToSave * 3)
            {
                StructureGroup group;

                for (group = this.newestGroup; group != null && groupsToSave > 0; --groupsToSave)
                {
                    group = group.olderGroup;
                }

                if (group != null)
                {
                    if (group.newerGroup == null)
                    {
                        this.newestGroup = null;
                    }
                    else
                    {
                        group.newerGroup.olderGroup = null;
                    }

                    this.oldestGroup = group.newerGroup;
                    group.newerGroup = null;

                    while (group != null)
                    {
                        Long key = Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(group.chunkX, group.chunkZ));
                        super.structureMap.remove(key);
                        group = group.olderGroup;
                    }
                }
            }
        }
    }

    public void clear()
    {
        if (this._canGenerate)
        {
            super.structureMap.clear();
            this.newestGroup = this.oldestGroup = null;
            this.debuggingGeometryMap.clear();
            this.completedStructures = this.populatedChunks = 0;
            this.completedStructureBlocks = this.placedBlocks = 0L;
        }
    }

    public GeometryStream getDebuggingGeometry(World world, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid)
        {
            if (world != super.worldObj)
            {
                return null;
            }
            else
            {
                long key = (long)chunkX << 32 | (long)chunkZ & 4294967295L;
                return (GeometryStream)this.debuggingGeometryMap.get(Long.valueOf(key));
            }
        }
        else
        {
            return null;
        }
    }

    public boolean validate() throws IllegalStateException
    {
        this.clear();
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

        return this._valid && this._canGenerate;
    }

    protected StructureGroup getCachedStructureGroup(int chunkX, int chunkZ)
    {
        Long key = Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        StructureGroup group = (StructureGroup)super.structureMap.get(key);

        if (group != null)
        {
            StructureGroup older = group.olderGroup;
            StructureGroup newer = group.newerGroup;

            if (older == null)
            {
                this.oldestGroup = newer;
            }
            else
            {
                older.newerGroup = newer;
            }

            if (newer == null)
            {
                this.newestGroup = older;
            }
            else
            {
                newer.olderGroup = older;
            }

            group.newerGroup = null;
            group.olderGroup = this.newestGroup;

            if (this.newestGroup == null)
            {
                this.oldestGroup = group;
            }
            else
            {
                this.newestGroup.newerGroup = group;
            }

            this.newestGroup = group;
        }

        return group;
    }

    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int rootX, int rootZ, byte[] chunkBlocks)
    {
        if (this.parent != null)
        {
            int group = this.parent.range;
            this.parent.range = ((int)this.parentRangeLimit.getMax() + 15) / 16;
            this.parent.generate(world, chunkX, chunkZ);
            this.parent.range = group;
        }

        super.rand.setSeed((long)super.rand.nextInt() ^ this.seed);
        super.rand.nextInt();
        StructureGroup group1 = this.getCachedStructureGroup(chunkX, chunkZ);

        if (group1 == null)
        {
            super.rand.nextInt();

            if (this.canSpawnStructureAtCoords(chunkX, chunkZ))
            {
                group1 = (StructureGroup)this.getStructureStart(chunkX, chunkZ);
                Long key = Long.valueOf(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
                super.structureMap.put(key, group1);
            }
        }
    }

    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ)
    {
        return this._canGenerate && this._valid ? (this.frequency.getMax() >= 1.0F ? true : this.frequency.getIntValue(super.rand) == 1) : false;
    }

    protected StructureStart getStructureStart(int chunkX, int chunkZ)
    {
        int count = this.frequency.getMax() >= 1.0F ? this.frequency.getIntValue(super.rand) : 1;
        StructureGroup group = new StructureGroup(chunkX, chunkZ, count);
        group.newerGroup = null;
        group.olderGroup = this.newestGroup;

        if (this.newestGroup == null)
        {
            this.oldestGroup = group;
        }
        else
        {
            this.newestGroup.newerGroup = group;
        }

        this.newestGroup = group;
        return group;
    }

    public abstract Component generateStructure(StructureGroup var1, Random var2);

    public boolean generateStructuresInChunk(World world, Random random, int chunkX, int chunkZ)
    {
        if (this._canGenerate && this._valid)
        {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            StructureBoundingBox bb = new StructureBoundingBox(minX, 0, minZ, minX + 15, world.getHeight(), minZ + 15);
            boolean structureFound = false;

            for (int cX = chunkX - super.range; cX <= chunkX + super.range; ++cX)
            {
                for (int cZ = chunkZ - super.range; cZ <= chunkZ + super.range; ++cZ)
                {
                    StructureGroup group = this.getCachedStructureGroup(cX, cZ);

                    if (group != null && group.isSizeableStructure() && group.getBoundingBox().intersectsWith(bb))
                    {
                        group.generateStructure(world, random, bb);
                        structureFound = true;
                    }
                }
            }

            ++this.populatedChunks;
            return structureFound;
        }
        else
        {
            return false;
        }
    }

    public ChunkPosition getNearestStructure(World world, int x, int y, int z)
    {
        if (this._canGenerate && this._valid)
        {
            ChunkPosition minPos = null;
            int minDist2 = Integer.MAX_VALUE;
            StructureBoundingBox searchBounds = new StructureBoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            for (StructureGroup vs : (Collection<StructureGroup>)super.structureMap.values()) {
            	if (vs.getBoundingBox().intersectsWith(searchBounds))
                {
                	for (Component vc : (List<Component>)vs.getComponents()) {
                		if (vc.getComponentType() == 0)
                        {
                            ChunkPosition center = vc.getCenter();
                            int dist2 = (center.x - x) * (center.x - x) + (center.z - z) * (center.z - z);

                            if (dist2 < minDist2)
                            {
                                minPos = center;
                                minDist2 = dist2;
                                int dist = (int)Math.sqrt((double)dist2) + 1;
                                searchBounds.minX = x - dist;
                                searchBounds.minZ = z - dist;
                                searchBounds.maxX = x + dist;
                                searchBounds.maxZ = z + dist;
                            }
                        }
                    }
                }
            }
            
            return minPos;
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        return this.name;
    }

    public class StructureGroup extends StructureStart
    {
        public final int structureCount;
        public int completeComponents;
        public long completeComponentBlocks;
        public final int chunkX;
        public final int chunkZ;
        private StructureGroup newerGroup;
        private StructureGroup olderGroup;

        public StructureGroup(int chunkX, int chunkZ, int structureCount)
        {
            this.completeComponents = 0;
            this.completeComponentBlocks = 0L;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            int trueStructureCount = 0;

            for (int i = 0; i < structureCount; ++i)
            {
                Random random = new Random(rand.nextLong());

                if (MapGenOreDistribution.this.generateStructure(this, random) != null)
                {
                    ++trueStructureCount;
                }
            }

            this.structureCount = trueStructureCount;
            this.updateBoundingBox();

            if (ServerState.getWorldConfig(worldObj).debuggingMode && (wfHasBB || wfHasWireframe))
            {
                this.buildWireframes();
            }
        }

        public boolean isSizeableStructure()
        {
            return true;
        }

        public void addComponent(Component component, Component parent)
        {
            super.components.add(component);
            component.setParent(parent);

            if (parent != null)
            {
                parent.setChild(component);
            }
        }

        public boolean canPlaceComponentAt(int componentType, float x, float y, float z, Random random)
        {
            int iX = MathHelper.floor_float(x);
            int iY = MathHelper.floor_float(y);
            int iZ = MathHelper.floor_float(z);

            if (componentType == 0)
            {
                BiomeGenBase dist = worldObj.getBiomeGenForCoords(iX, iZ);

                if (dist != null && !biomes.matchesBiome(dist, random))
                {
                    return false;
                }
            }

            if (componentType == 0)
            {
                float dist1 = parentRangeLimit.getValue(random);

                if (parent != null)
                {
                    if (dist1 < 0.0F)
                    {
                        return false;
                    }

                    ChunkPosition parentPos = parent.getNearestStructure(worldObj, iX, 0, iZ);

                    if (parentPos == null)
                    {
                        return false;
                    }

                    float dx = (float)(parentPos.x - iX);
                    float dz = (float)(parentPos.z - iZ);

                    if (dx * dx + dz * dz > dist1 * dist1)
                    {
                        return false;
                    }
                }
            }

            return true;
        }

        public void generateStructure(World world, Random random, StructureBoundingBox bounds)
        {
            int oldCompleteComponents = this.completeComponents;
            super.generateStructure(world, random, bounds);

            if (oldCompleteComponents != this.completeComponents && this.completeComponents == super.components.size())
            {
                completedStructures += this.structureCount;
                completedStructureBlocks += this.completeComponentBlocks;
            }
        }

        public void buildWireframes()
        {
            GeometryStream builder;

            for (Component comp : (List<Component>)this.getComponents()) {
            	StructureBoundingBox bb = comp.getBoundingBox();
                int cX = bb.getCenterX() / 16;
                int cZ = bb.getCenterZ() / 16;
                long key = (long)cX << 32 | (long)cZ & 4294967295L;
                builder = (GeometryStream)debuggingGeometryMap.get(Long.valueOf(key));

                if (builder == null)
                {
                    builder = new GeometryStream();
                    debuggingGeometryMap.put(Long.valueOf(key), builder);
                }
                comp.buildWireframe(builder);
            }
        }
    }
    
    public class Component extends StructureComponent
    {
        public final StructureGroup structureGroup;
        public long populatedBlocks;
        public long placedBlocks;

        public Component(StructureGroup structureGroup)
        {
            super(0);
            this.populatedBlocks = 0L;
            this.placedBlocks = 0L;
            this.structureGroup = structureGroup;
        }

        public boolean addComponentParts(World world, Random random, StructureBoundingBox bounds)
        {
            int sizeX = Math.min(bounds.maxX, super.boundingBox.maxX) - Math.max(bounds.minX, super.boundingBox.minX) + 1;
            int sizeY = Math.min(bounds.maxY, super.boundingBox.maxY) - Math.max(bounds.minY, super.boundingBox.minY) + 1;
            int sizeZ = Math.min(bounds.maxZ, super.boundingBox.maxZ) - Math.max(bounds.minZ, super.boundingBox.minZ) + 1;

            if (sizeX > 0 && sizeY > 0 && sizeZ > 0)
            {
                this.populatedBlocks += (long)(sizeX * sizeY * sizeZ);
                sizeX = super.boundingBox.maxX - super.boundingBox.minX + 1;
                sizeY = Math.min(world.getHeight() - 1, super.boundingBox.maxY) - Math.max(0, super.boundingBox.minY) + 1;
                sizeZ = super.boundingBox.maxZ - super.boundingBox.minZ + 1;
                long totalVolume = (long)(sizeX * sizeY * sizeZ);

                if (this.populatedBlocks == totalVolume && this.structureGroup != null)
                {
                	// TODO: this is where can log a row in the debugging table
                	// Record: some x/y/z of component (add this), biome (lookup), attempted (record) and placed blocks
                    ++this.structureGroup.completeComponents;
                    this.structureGroup.completeComponentBlocks += this.placedBlocks;
                }

                return true;
            }
            else
            {
                return true;
            }
        }

        public boolean attemptPlaceBlock(World world, Random random, int x, int y, int z, StructureBoundingBox bounds)
        {
            if (!bounds.isVecInside(x, y, z))
            {
                return false;
            }
            else
            {
                Chunk chunk = world.getChunkFromBlockCoords(x, z);
                int cx = x & 15;
                int cz = z & 15;
                int currentBlock = chunk.getBlockID(cx, y, cz);
                int fastCheck = replaceableBlocks.matchesBlock_fast(currentBlock);

                if (fastCheck == 0)
                {
                    return false;
                }
                else if (fastCheck == -1 && !replaceableBlocks.matchesBlock(currentBlock, chunk.getBlockMetadata(cx, y, cz), random))
                {
                    return false;
                }
                else
                {
                    int match = oreBlock.getMatchingBlock(random);

                    if (match == -1)
                    {
                        return false;
                    }
                    else
                    {
                        boolean placed = chunk.setBlockIDWithMetadata(cx, y, cz, match >>> 16, match & 65535);

                        if (placed)
                        {
                            ++this.placedBlocks;
                            ++MapGenOreDistribution.this.placedBlocks;
                            world.markBlockForUpdate(x, y, z);
                        }

                        return placed;
                    }
                }
            }
        }

        public void setParent(Component parent)
        {
            if (parent != null)
            {
                super.componentType = parent.componentType + 1;
            }
            else
            {
                super.componentType = 0;
            }
        }

        public void setChild(Component child)
        {
            if (child != null)
            {
                child.componentType = super.componentType + 1;
            }
        }

        public void buildWireframe(IGeometryBuilder gb)
        {
            float[] color = new float[4];

            if (wfHasBB)
            {
                color[3] = (float)(wfBBColor >>> 24 & 255L) / 255.0F;
                color[0] = (float)(wfBBColor >>> 16 & 255L) / 255.0F;
                color[1] = (float)(wfBBColor >>> 8 & 255L) / 255.0F;
                color[2] = (float)(wfBBColor & 255L) / 255.0F;
                gb.setColor(color);
                StructureBoundingBox bounds = this.getBoundingBox();
                Transform trans = new Transform();
                trans.scale(0.5F, 0.5F, 0.5F);
                trans.translate((float)(bounds.maxX + bounds.minX), (float)(bounds.maxY + bounds.minY), (float)(bounds.maxZ + bounds.minZ));
                trans.scale((float)(bounds.maxX - bounds.minX), (float)(bounds.maxY - bounds.minY), (float)(bounds.maxZ - bounds.minZ));
                gb.setPositionTransform(trans);
                WireframeShapes.addUnitWireCube(gb);
            }

            if (wfHasWireframe)
            {
                color[3] = (float)(wfWireframeColor >>> 24 & 255L) / 255.0F;
                color[0] = (float)(wfWireframeColor >>> 16 & 255L) / 255.0F;
                color[1] = (float)(wfWireframeColor >>> 8 & 255L) / 255.0F;
                color[2] = (float)(wfWireframeColor & 255L) / 255.0F;
                gb.setColor(color);
            }
        }
    }

}
