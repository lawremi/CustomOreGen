package CustomOreGen.Server;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockArrangement;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.HeightScaledPDist;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.TileEntityHelper;
import CustomOreGen.Util.TouchingDescriptorList;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.WireframeShapes;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.structure.IStructurePieceType;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;

public abstract class MapGenOreDistribution extends MapGenStructure implements IOreDistribution
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
	public final BlockDescriptor oreBlock = new BlockDescriptor();

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
			name = "DistributionFrequency",
			info = "Number of distribution structures per 16x16 chunk"
			)
	public final HeightScaledPDist frequency;

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
			name = "MinHeight",
			info = "Minimum absolute height allowed"
			)
	public int minHeight;

	@DistributionSetting(
			name = "MaxHeight",
			info = "Maximum absolute height allowed"
			)
	public int maxHeight;

	@DistributionSetting(
			name = "HeightOffset",
			info = "Number, in blocks, to add to the scaled height"
			)
	public PDist heightOffset;

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

	@DistributionSetting(
			name = "version",
			info = "Version of the distribution configuration."
			)
	public long version;

	protected Map<Long,GeometryStream> debuggingGeometryMap;
	protected boolean _valid;
	protected final boolean _canGenerate;
	private StructureGroup newestGroup;
	protected final DistributionSettingMap _settingMap;

	public MapGenOreDistribution(DistributionSettingMap settingMap, int distributionID, boolean canGenerate)
	{
		this.replaceableBlocks = new BlockDescriptor(Blocks.STONE);
		this.aboveBlocks = new BlockDescriptor();
		this.belowBlocks = new BlockDescriptor();
		this.besideBlocks = new BlockDescriptor();
		this.touchingBlocks = new TouchingDescriptorList();
		this.biomes = new BiomeDescriptor(".*");
		this.frequency = new HeightScaledPDist(0.025F, 0.0F);
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
		this.debuggingGeometryMap = new HashMap<Long,GeometryStream>();
		this._valid = false;
		this.newestGroup = null;
		this.name = "Distribution_" + distributionID;
		this.seed = (new Random((long)distributionID)).nextLong();
		this._canGenerate = canGenerate;
		this._settingMap = settingMap;
		this.minHeight = 0;
		this.maxHeight = 256;
		this.heightOffset = new PDist();
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

	public Map<String,String> getDistributionSettingDescriptions()
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

	public synchronized void generate(IWorld world, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			if (world != super.world)
			{
				this.clear();
			}

			this.generate(world, chunkX, chunkZ, new ChunkPrimer(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY));
		}
	}

	public synchronized void populate(IWorld world, int chunkX, int chunkZ)
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

	@SuppressWarnings("deprecation")
	public synchronized void cull()
	{
		if (this._canGenerate)
		{
			int groupsToSave = (int)(6.0F * Math.min(1.0F, this.frequency.pdist.getMax()) * (float)(2 * super.range + 1) * (float)(2 * super.range + 1));

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

					group.newerGroup = null;

					while (group != null)
					{
						Long key = Long.valueOf(ChunkPos.asLong(group.getChunkPosX(), group.getChunkPosZ()));
						super.structureMap.remove(key);
						group = group.olderGroup;
					}
				}
			}
		}
	}

	public synchronized void clear()
	{
		if (this._canGenerate)
		{
			super.structureMap.clear();
			this.newestGroup = null;
			this.debuggingGeometryMap.clear();
			this.completedStructures = this.populatedChunks = 0;
			this.completedStructureBlocks = this.placedBlocks = 0L;
		}
	}

	public abstract double getAverageOreCount();

	@Override
	public double getOresPerChunk() {
		return this.frequency.pdist.mean * getAverageOreCount();
	}

	@Override
	public GeometryStream getDebuggingGeometry(IWorld world, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			if (world != super.world)
			{
				return null;
			}
			else
			{
				long key = (long)chunkX << Integer.SIZE | (long)chunkZ & 4294967295L;
				return this.debuggingGeometryMap.get(key);
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
		else if (oreBlockMatchWeight > 1.0002F)
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

		if (this.minHeight > this.maxHeight)
		{
			this._valid = false;
			throw new IllegalStateException("Invalid height range [" + this.minHeight + "," + this.maxHeight + "] for " + this);
		}

		return this._valid && this._canGenerate;
	}

	private StructureGroup getCachedStructureGroup(int chunkX, int chunkZ)
	{
		long key = ChunkPos.asLong(chunkX, chunkZ);
		StructureGroup group = (StructureGroup)super.structureMap.get(key);

		if (group != null)
		{
			StructureGroup older = group.olderGroup;
			StructureGroup newer = group.newerGroup;

			if (older == null)
			{
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
			}
			else
			{
				this.newestGroup.newerGroup = group;
			}

			this.newestGroup = group;
		}

		return group;
	}

	// FIXME: copy-and-pasted this from MapGenBase, because MapGenStructure now declares recursiveGenerate as 'final'. 
	// We worked around this by renaming to recursiveGenerate2, which is called by this method. 
	@Override
	public void generate(IWorld par2World, int par3, int par4, ChunkPrimer primer)
	{
		int k = 0;//this.range;
		this.world = par2World;
		this.rand.setSeed(par2World.getSeed());
		long l = this.rand.nextLong();
		long i1 = this.rand.nextLong();

		for (int j1 = par3 - k; j1 <= par3 + k; ++j1)
		{
			for (int k1 = par4 - k; k1 <= par4 + k; ++k1)
			{
				long l1 = (long)j1 * l;
				long i2 = (long)k1 * i1;
				this.rand.setSeed(l1 ^ i2 ^ par2World.getSeed());
				this.recursiveGenerate2(par2World, j1, k1, par3, par4, primer);
			}
		}
	}

	protected void recursiveGenerate2(IWorld world, int chunkX, int chunkZ, int rootX, int rootZ, ChunkPrimer primer)
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
				long key = ChunkPos.asLong(chunkX, chunkZ);
				super.structureMap.put(key, group1);
			}
		}
	}

	protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ)
	{
		int blockX = chunkX << 4;
		int blockZ = chunkZ << 4;
		boolean canSpawn = false;
		if (this._canGenerate && this._valid) {
			if (this.frequency.getMax(this.world.getWorld(), blockX, blockZ) >= 1.0F) {
				canSpawn = true; 
			} else {
				canSpawn = this.frequency.getIntValue(super.rand, this.world.getWorld(), blockX, blockZ) == 1;
			}
		}
		return canSpawn;
	}

	protected StructureStart getStructureStart(int chunkX, int chunkZ)
	{
		int blockX = chunkX << 4;
		int blockZ = chunkZ << 4;
		int count = this.frequency.getMax(this.world.getWorld(), blockX, blockZ) >= 1.0F ? 
				this.frequency.getIntValue(super.rand, this.world.getWorld(), blockX, blockZ) : 1;
				StructureGroup group = new StructureGroup(chunkX, chunkZ, count);
				group.newerGroup = null;
				group.olderGroup = this.newestGroup;

				if (this.newestGroup == null)
				{
				}
				else
				{
					this.newestGroup.newerGroup = group;
				}

				this.newestGroup = group;
				return group;
	}

	public abstract Component generateStructure(StructureGroup structureGroup, Random rand);

	private boolean generateStructuresInChunk(IWorld world, Random random, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			int minX = chunkX << 4;
			int minZ = chunkZ << 4;
			MutableBoundingBox bb = new MutableBoundingBox(minX, 0, minZ, minX + 15, world.getHeight(), minZ + 15);
			boolean structureFound = false;

			for (int cX = chunkX - super.range; cX <= chunkX + super.range; ++cX)
			{
				for (int cZ = chunkZ - super.range; cZ <= chunkZ + super.range; ++cZ)
				{
					StructureGroup group = this.getCachedStructureGroup(cX, cZ);

					if (group != null && group.isSizeableStructure() && group.getBoundingBox().intersectsWith(bb))
					{
						group.generateStructure(world, random, bb, new ChunkPos(chunkX, chunkZ));
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

	public BlockPos getNearestStructure(IWorld world, BlockPos pos)
	{
		if (this._canGenerate && this._valid)
		{
			BlockPos minPos = null;
			int minDist2 = Integer.MAX_VALUE;
			MutableBoundingBox searchBounds = new MutableBoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			for (StructureStart vs : (Collection<StructureStart>)super.structureMap.values()) {
				if (vs.getBoundingBox().intersectsWith(searchBounds))
				{
					for (StructurePiece vc : (List<StructurePiece>)vs.getComponents()) {
						if (vc.getComponentType() == 0)
						{
							BlockPos center = getBoundingBoxCenter(vc.getBoundingBox());
							int dist2 = (center.getX() - pos.getX()) * (center.getX() - pos.getX()) + 
									(center.getZ() - pos.getZ()) * (center.getZ() - pos.getZ());

							if (dist2 < minDist2)
							{
								minPos = center;
								minDist2 = dist2;
								int dist = (int)Math.sqrt((double)dist2) + 1;
								searchBounds.minX = pos.getX() - dist;
								searchBounds.minZ = pos.getZ() - dist;
								searchBounds.maxX = pos.getX() + dist;
								searchBounds.maxZ = pos.getZ() + dist;
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

	private BlockPos getBoundingBoxCenter(MutableBoundingBox box) {
		return new BlockPos((box.maxX - box.minX)/2 + box.minX, (box.maxY - box.minY)/2 + box.minY, (box.maxZ - box.minZ)/2 + box.minZ);
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
		private StructureGroup newerGroup;
		private StructureGroup olderGroup;

		public StructureGroup(int chunkX, int chunkZ, int structureCount)
		{
			super(null, chunkX, chunkZ, null, null, structureCount, seed);
			this.completeComponents = 0;
			this.completeComponentBlocks = 0L;
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

			if (ServerState.getWorldConfig(world.getWorld()).debuggingMode && (wfHasBB || wfHasWireframe))
			{
				this.buildWireframes();
			}
		}

		/**
		 * Calculates total bounding box based on components' bounding boxes and saves it to boundingBox
		 */
		protected void updateBoundingBox()
		{
			this.bounds = new MutableBoundingBox(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

			for (StructurePiece structurecomponent : this.components)
			{
				this.bounds.expandTo(structurecomponent.getBoundingBox());
			}
		}

		/*public StructureGroup(Structure<?> structure, int chunkX, int chunkZ, Biome biome, MutableBoundingBox bounds, int referenceIn, long seed)
        {
            super(structure, chunkX, chunkZ, biome, bounds, referenceIn, seed);
            structureCount = 0;
        }*/

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
			int iX = MathHelper.floor(x);
			int iY = MathHelper.floor(y);
			int iZ = MathHelper.floor(z);

			BlockPos pos = new BlockPos(iX, iY, iZ);

			if (componentType == 0)
			{
				Biome dist = world.getBiome(pos);

				if (dist != null && !biomes.matchesBiome(dist, random))
				{
					return false;
				}
			}

			if (componentType == 0)
			{
				if (iY < minHeight || iY > maxHeight) {
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

					BlockPos parentPos = parent.getNearestStructure(world, pos);

					if (parentPos == null)
					{
						return false;
					}

					float dx = (float)(parentPos.getX() - iX);
					float dz = (float)(parentPos.getZ() - iZ);

					if (dx * dx + dz * dz > dist1 * dist1)
					{
						return false;
					}
				}
			}

			return true;
		}

		@Override
		public void generateStructure(IWorld world, Random random, MutableBoundingBox bounds, ChunkPos pos)
		{
			int oldCompleteComponents = this.completeComponents;
			super.generateStructure(world, random, bounds, pos);

			if (oldCompleteComponents != this.completeComponents && this.completeComponents == super.components.size())
			{
				completedStructures += this.structureCount;
				completedStructureBlocks += this.completeComponentBlocks;
			}
		}

		public void buildWireframes()
		{
			GeometryStream builder;

			for (StructurePiece comp : this.getComponents()) {
				MutableBoundingBox bb = comp.getBoundingBox();
				int cX = getBoundingBoxCenter(bb).getX() / 16;
				int cZ = getBoundingBoxCenter(bb).getZ() / 16;
				long key = (long)cX << 32 | (long)cZ & 4294967295L;
				builder = debuggingGeometryMap.get(key);

				if (builder == null)
				{
					builder = new GeometryStream();
					debuggingGeometryMap.put(key, builder);
				}
				((Component)comp).buildWireframe(builder);
			}
		}

		@Override
		public void init(ChunkGenerator<?> generator, TemplateManager templateManagerIn, int chunkX, int chunkZ, Biome biomeIn) {
			// TODO Auto-generated method stub

		}
	}

	public class Component extends StructurePiece
	{
		public final StructureGroup structureGroup;
		public long populatedBlocks;
		public long placedBlocks;

		public Component(StructureGroup structureGroup)
		{
			super(IStructurePieceType.BTP,0);//TODO: need a structure piece type
			this.populatedBlocks = 0L;
			this.placedBlocks = 0L;
			this.structureGroup = structureGroup;
		}

		@Override
		public boolean addComponentParts(IWorld worldIn, Random randomIn, MutableBoundingBox bounds, ChunkPos pos)
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

		public boolean attemptPlaceBlock(IWorld world, Random random, int x, int y, int z, MutableBoundingBox bounds)
		{
			BlockPos pos = new BlockPos(x, y, z);
			if (!bounds.isVecInside(pos))
			{
				return false;
			}
			else
			{
				BlockArrangement arrangement = new BlockArrangement(replaceableBlocks, aboveBlocks, belowBlocks, besideBlocks, touchingBlocks);
				boolean matched = arrangement.matchesAt(world, random, pos);
				if (matched)
				{
					BlockInfo match = oreBlock.getMatchingBlock(random);

					if (match == null)
					{
						return false;
					}
					else
					{
						boolean placed = world.setBlockState(pos, match.getBlockState(), 2);

						if (placed)
						{
							TileEntityHelper.readFromPartialNBT(world.getWorld(), x, y, z, match.getNBT());
							++this.placedBlocks;
							++MapGenOreDistribution.this.placedBlocks;
						}

						return placed;
					}
				}
				return false;
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
				MutableBoundingBox bounds = this.getBoundingBox();
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

		@Override
		protected void readAdditional(CompoundNBT tagCompound) {
			// TODO Auto-generated method stub
		}
	}

	@Override
	public BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored) {
		this.world = worldIn;
		this.initializeStructureData(worldIn);
		this.rand.setSeed(worldIn.getSeed());
		long i = this.rand.nextLong();
		long j = this.rand.nextLong();
		long k = (long)(pos.getX() >> 4) * i;
		long l = (long)(pos.getZ() >> 4) * j;
		this.rand.setSeed(k ^ l ^ worldIn.getSeed());
		this.recursiveGenerate(worldIn, pos.getX() >> 4, pos.getZ() >> 4, 0, 0, (ChunkPrimer)null);
		double d0 = Double.MAX_VALUE;
		BlockPos blockpos = null;

		for (StructureStart structurestart : this.structureMap.values())
		{
			if (structurestart.isValid())
			{
				StructurePiece structurecomponent = (StructurePiece)structurestart.getComponents().get(0);
				BlockPos blockpos1 = getBoundingBoxCenter(structurecomponent.getBoundingBox());
				double d1 = blockpos1.distanceSq(pos);

				if (d1 < d0)
				{
					d0 = d1;
					blockpos = blockpos1;
				}
			}
		}
		if (blockpos != null)
		{
			return blockpos;
		}
		else
		{
			return null;
		}
	}
}
