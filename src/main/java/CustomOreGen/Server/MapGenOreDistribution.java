package CustomOreGen.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import CustomOreGen.Server.DistributionSettingMap.DistributionSetting;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockArrangement;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import CustomOreGen.Util.BlockEntityHelper;
import CustomOreGen.Util.GeometryStream;
import CustomOreGen.Util.HeightScaledPDist;
import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;
import CustomOreGen.Util.TouchingDescriptorList;
import CustomOreGen.Util.Transform;
import CustomOreGen.Util.WireframeShapes;
import CustomOreGen.Util.WorldGenQueries;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

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
	protected final DistributionSettingMap _settingMap;

	/**
	 * Structure groups by origin chunk, oldest-used first. Groups are rebuilt deterministically from the
	 * world seed, so evicting one only costs the time to regenerate it.
	 */
	protected final Long2ObjectLinkedOpenHashMap<StructureGroup> structureMap = new Long2ObjectLinkedOpenHashMap<StructureGroup>(1024);

	/** Origin chunks whose groups have already added their wireframes to debuggingGeometryMap. */
	protected final LongSet wireframedGroups = new LongOpenHashSet();

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
		this.name = "Distribution_" + distributionID;
		this.seed = (RandomSource.create((long)distributionID)).nextLong();
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

	/**
	 * Builds (or fetches from cache) every structure group whose origin chunk is within range of this
	 * chunk. Layout only; nothing is placed. Height and biome lookups go through the chunk generator
	 * (see WorldGenQueries), so origins outside the chunk's 3x3 generation region are fine.
	 */
	public synchronized void generate(LevelAccessor world, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			// Always lay out against the ServerLevel: a WorldGenRegion is a different object for every
			// chunk, and must not be asked about positions outside its region anyway.
			ServerLevel level = WorldGenQueries.serverLevel(world);
			LevelAccessor layoutWorld = level != null ? level : world;

			if (layoutWorld != super.world)
			{
				this.clear();
			}

			this.generate(layoutWorld, chunkX, chunkZ, (ChunkAccess)null);
		}
	}

	/**
	 * Places the parts of every cached structure group that fall inside this chunk's 16x16 column.
	 * Blocks are never written outside that column, so each block is placed exactly once, when its own
	 * chunk is populated (see ServerState.onChunkFeaturesDone for when that is).
	 */
	public synchronized void populate(WorldGenLevel world, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			long worldSeed = world.getSeed();
			RandomSource random = RandomSource.create(worldSeed);
			long xSeed = random.nextLong() >> 3;
			long zSeed = random.nextLong() >> 3;
			random.setSeed(xSeed * (long)chunkX + zSeed * (long)chunkZ ^ worldSeed ^ this.seed);
			this.generateStructuresInChunk(world, random, chunkX, chunkZ);
		}
	}

	public synchronized void cull()
	{
		if (this._canGenerate)
		{
			int groupsToSave = (int)(6.0F * Math.min(1.0F, this.frequency.pdist.getMax()) * (float)(2 * super.range + 1) * (float)(2 * super.range + 1));
			// Worldgen runs on several threads at once, each working on a different neighborhood, so keep
			// a generous number of recently-used groups rather than just one neighborhood's worth.
			int maxGroups = Math.max(groupsToSave * 16, 4096);

			while (this.structureMap.size() > maxGroups)
			{
				this.structureMap.removeFirst();
			}
		}
	}

	public synchronized void clear()
	{
		if (this._canGenerate)
		{
			this.structureMap.clear();
			this.debuggingGeometryMap.clear();
			this.wireframedGroups.clear();
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
	public synchronized GeometryStream getDebuggingGeometry(LevelAccessor world, int chunkX, int chunkZ)
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
		// Moves the group to the most-recently-used end, so cull() evicts the least recently used.
		return this.structureMap.getAndMoveToLast(ChunkPos.asLong(chunkX, chunkZ));
	}

	// FIXME: copy-and-pasted this from MapGenBase, because MapGenStructure now declares recursiveGenerate as 'final'.
	// We worked around this by renaming to recursiveGenerate2, which is called by this method.
	@Override
	public void generate(LevelAccessor par2Level, int par3, int par4, ChunkAccess primer)
	{
		// Every origin chunk whose structures could reach this chunk must be laid out, not just this
		// one; otherwise a vein starting next door only shows up here if next door generated first.
		int k = this.range;
		this.world = par2Level;
		long worldSeed = getWorldSeed(par2Level);
		this.rand.setSeed(worldSeed);
		long l = this.rand.nextLong();
		long i1 = this.rand.nextLong();

		for (int j1 = par3 - k; j1 <= par3 + k; ++j1)
		{
			for (int k1 = par4 - k; k1 <= par4 + k; ++k1)
			{
				long l1 = (long)j1 * l;
				long i2 = (long)k1 * i1;
				this.rand.setSeed(l1 ^ i2 ^ worldSeed);
				this.recursiveGenerate2(par2Level, j1, k1, par3, par4, primer);
			}
		}
	}

	private static long getWorldSeed(LevelAccessor world)
	{
		if (world instanceof WorldGenLevel level)
		{
			return level.getSeed();
		}
		return world.getServer().getWorldData().worldGenOptions().seed();
	}

	protected void recursiveGenerate2(LevelAccessor world, int chunkX, int chunkZ, int rootX, int rootZ, ChunkAccess primer)
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
				group1 = this.createStructureGroup(chunkX, chunkZ);
				this.structureMap.putAndMoveToLast(ChunkPos.asLong(chunkX, chunkZ), group1);
			}
		}
	}

	protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ)
	{
		int blockX = chunkX << 4;
		int blockZ = chunkZ << 4;
		boolean canSpawn = false;
		if (this._canGenerate && this._valid) {
			if (this.frequency.getMax(this.world, blockX, blockZ) >= 1.0F) {
				canSpawn = true;
			} else {
				canSpawn = this.frequency.getIntValue(super.rand, this.world, blockX, blockZ) == 1;
			}
		}
		return canSpawn;
	}

	protected StructureGroup createStructureGroup(int chunkX, int chunkZ)
	{
		int blockX = chunkX << 4;
		int blockZ = chunkZ << 4;
		int count = this.frequency.getMax(this.world, blockX, blockZ) >= 1.0F ?
				this.frequency.getIntValue(super.rand, this.world, blockX, blockZ) : 1;
		return new StructureGroup(chunkX, chunkZ, count);
	}

	public abstract Component generateStructure(StructureGroup structureGroup, RandomSource rand);

	private boolean generateStructuresInChunk(WorldGenLevel world, RandomSource random, int chunkX, int chunkZ)
	{
		if (this._canGenerate && this._valid)
		{
			int minX = chunkX << 4;
			int minZ = chunkZ << 4;
			BoundingBox bb = new BoundingBox(minX, world.getMinBuildHeight(), minZ, minX + 15, world.getMaxBuildHeight() - 1, minZ + 15);
			ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
			boolean structureFound = false;

			for (int cX = chunkX - super.range; cX <= chunkX + super.range; ++cX)
			{
				for (int cZ = chunkZ - super.range; cZ <= chunkZ + super.range; ++cZ)
				{
					StructureGroup group = this.getCachedStructureGroup(cX, cZ);

					if (group != null && group.intersects(bb))
					{
						group.generateStructure(world, random, bb, chunkPos);
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

	public synchronized BlockPos getNearestStructure(LevelAccessor world, BlockPos pos)
	{
		if (this._canGenerate && this._valid)
		{
			BlockPos minPos = null;
			int minDist2 = Integer.MAX_VALUE;

			for (StructureGroup group : this.structureMap.values()) {
				for (Component vc : group.components) {
					if (vc.getGenDepth() == 0)
					{
						BlockPos center = getBoundingBoxCenter(vc.getBoundingBox());
						int dist2 = (center.getX() - pos.getX()) * (center.getX() - pos.getX()) +
								(center.getZ() - pos.getZ()) * (center.getZ() - pos.getZ());

						if (dist2 < minDist2)
						{
							minPos = center;
							minDist2 = dist2;
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

	private BlockPos getBoundingBoxCenter(BoundingBox box) {
		return new BlockPos((box.maxX() - box.minX())/2 + box.minX(), (box.maxY() - box.minY())/2 + box.minY(), (box.maxZ() - box.minZ())/2 + box.minZ());
	}

	public String toString()
	{
		return this.name;
	}

	/**
	 * All the structures (motherlodes, veins, clouds, clusters) that originate in one chunk. Replaces the
	 * old StructureStart subclass; vanilla's StructureStart can no longer be extended or added to.
	 */
	public class StructureGroup
	{
		public final int chunkX;
		public final int chunkZ;
		public final int structureCount;
		public final List<Component> components = new ArrayList<Component>();
		public int completeComponents;
		public long completeComponentBlocks;
		private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

		public StructureGroup(int chunkX, int chunkZ, int structureCount)
		{
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
			this.completeComponents = 0;
			this.completeComponentBlocks = 0L;
			int trueStructureCount = 0;

			for (int i = 0; i < structureCount; ++i)
			{
				RandomSource random = RandomSource.create(rand.nextLong());

				if (MapGenOreDistribution.this.generateStructure(this, random) != null)
				{
					++trueStructureCount;
				}
			}

			this.structureCount = trueStructureCount;
			this.updateBoundingBox();

			if ((wfHasBB || wfHasWireframe) && ServerState.isDebuggingMode(world))
			{
				this.buildWireframes();
			}
		}

		public ChunkPos getChunkPos()
		{
			return new ChunkPos(this.chunkX, this.chunkZ);
		}

		/**
		 * Calculates total bounding box based on components' bounding boxes
		 */
		protected void updateBoundingBox()
		{
			for (Component component : this.components)
			{
				BoundingBox bb = component.getBoundingBox();
				this.minX = Math.min(this.minX, bb.minX());
				this.minY = Math.min(this.minY, bb.minY());
				this.minZ = Math.min(this.minZ, bb.minZ());
				this.maxX = Math.max(this.maxX, bb.maxX());
				this.maxY = Math.max(this.maxY, bb.maxY());
				this.maxZ = Math.max(this.maxZ, bb.maxZ());
			}
		}

		public boolean intersects(BoundingBox bb)
		{
			return !this.components.isEmpty() &&
					this.maxX >= bb.minX() && this.minX <= bb.maxX() &&
					this.maxY >= bb.minY() && this.minY <= bb.maxY() &&
					this.maxZ >= bb.minZ() && this.minZ <= bb.maxZ();
		}

		public void addComponent(Component component, Component parent)
		{
			this.components.add(component);
			component.setParent(parent);

			if (parent != null)
			{
				parent.setChild(component);
			}
		}

		public boolean canPlaceComponentAt(int componentType, float x, float y, float z, RandomSource random)
		{
			int iX = Mth.floor(x);
			int iY = Mth.floor(y);
			int iZ = Mth.floor(z);

			if (componentType == 0)
			{
				Biome biome = WorldGenQueries.getBiome(world, iX, iY, iZ);

				if (biome != null && !biomes.matchesBiome(biome, random))
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

					BlockPos parentPos = parent.getNearestStructure(world, new BlockPos(iX, iY, iZ));

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

		public void generateStructure(WorldGenLevel world, RandomSource random, BoundingBox bounds, ChunkPos pos)
		{
			int oldCompleteComponents = this.completeComponents;

			for (Component component : this.components)
			{
				if (component.getBoundingBox().intersects(bounds))
				{
					component.addComponentParts(world, random, bounds, pos);
				}
			}

			if (oldCompleteComponents != this.completeComponents && this.completeComponents == this.components.size())
			{
				completedStructures += this.structureCount;
				completedStructureBlocks += this.completeComponentBlocks;
			}
		}

		public void buildWireframes()
		{
			// A group evicted by cull() and rebuilt later must not add its wireframes a second time.
			if (!wireframedGroups.add(ChunkPos.asLong(this.chunkX, this.chunkZ)))
			{
				return;
			}

			GeometryStream builder;

			for (Component comp : this.components) {
				BoundingBox bb = comp.getBoundingBox();
				int cX = getBoundingBoxCenter(bb).getX() >> 4;
				int cZ = getBoundingBoxCenter(bb).getZ() >> 4;
				long key = (long)cX << 32 | (long)cZ & 4294967295L;
				builder = debuggingGeometryMap.get(key);

				if (builder == null)
				{
					builder = new GeometryStream();
					debuggingGeometryMap.put(key, builder);
				}
				comp.buildWireframe(builder);
			}
		}
	}

	public class Component extends StructurePiece
	{
		public final StructureGroup structureGroup;
		public long populatedBlocks;
		public long placedBlocks;

		public Component(StructureGroup structureGroup, BoundingBox boundingBox)
		{
			super(StructurePieceType.BURIED_TREASURE_PIECE, 0, boundingBox);//TODO: need a structure piece type
			this.populatedBlocks = 0L;
			this.placedBlocks = 0L;
			this.structureGroup = structureGroup;
		}

		@Override
		public void addChildren(StructurePiece p_226835_, StructurePieceAccessor p_226836_, RandomSource p_226837_) {
			//TODO: addComponentParts?
		}

		@Override
		public void postProcess(WorldGenLevel p_226769_, StructureManager p_226770_, ChunkGenerator p_226771_, RandomSource p_226772_, BoundingBox p_226773_, ChunkPos p_226774_, BlockPos p_226775_) {
			this.addComponentParts(p_226769_, p_226772_, p_226773_, p_226774_);
		}

		public boolean addComponentParts(LevelAccessor worldIn, RandomSource randomIn, BoundingBox bounds, ChunkPos pos)
		{
			int sizeX = Math.min(bounds.maxX(), super.boundingBox.maxX()) - Math.max(bounds.minX(), super.boundingBox.minX()) + 1;
			int sizeY = Math.min(bounds.maxY(), super.boundingBox.maxY()) - Math.max(bounds.minY(), super.boundingBox.minY()) + 1;
			int sizeZ = Math.min(bounds.maxZ(), super.boundingBox.maxZ()) - Math.max(bounds.minZ(), super.boundingBox.minZ()) + 1;

			if (sizeX > 0 && sizeY > 0 && sizeZ > 0)
			{
				this.populatedBlocks += (long)(sizeX * sizeY * sizeZ);
				sizeX = super.boundingBox.maxX() - super.boundingBox.minX() + 1;
				sizeY = Math.min(worldIn.getMaxBuildHeight() - 1, super.boundingBox.maxY()) - Math.max(worldIn.getMinBuildHeight(), super.boundingBox.minY()) + 1;
				sizeZ = super.boundingBox.maxZ() - super.boundingBox.minZ() + 1;
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

		public boolean attemptPlaceBlock(LevelAccessor world, RandomSource random, int x, int y, int z, BoundingBox bounds)
		{
			BlockPos pos = new BlockPos(x, y, z);
			if (!bounds.isInside(pos))
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
						boolean placed = world.setBlock(pos, match.getBlockState(), 2);

						if (placed)
						{
							BlockEntityHelper.readFromPartialNBT(world, x, y, z, match.getNBT());
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
				super.genDepth = parent.genDepth + 1;
			}
			else
			{
				super.genDepth = 0;
			}
		}

		public void setChild(Component child)
		{
			if (child != null)
			{
				child.genDepth = super.genDepth + 1;
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
				BoundingBox bounds = this.getBoundingBox();
				Transform trans = new Transform();
				trans.scale(0.5F, 0.5F, 0.5F);
				trans.translate((float)(bounds.maxX() + bounds.minX()), (float)(bounds.maxY() + bounds.minY()), (float)(bounds.maxZ() + bounds.minZ()));
				trans.scale((float)(bounds.maxX() - bounds.minX()), (float)(bounds.maxY() - bounds.minY()), (float)(bounds.maxZ() - bounds.minZ()));
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
		protected void addAdditionalSaveData(StructurePieceSerializationContext p_192646_, CompoundTag p_192647_) {
			// TODO Auto-generated method stub
			
		}

		//@Override
		//protected void readAdditional(CompoundTag tagCompound) {
			// TODO Auto-generated method stub
		//}
	}

	@Override
	public BlockPos getNearestStructurePos(Level worldIn, BlockPos pos, boolean findUnexplored) {
		this.generate(worldIn, pos.getX() >> 4, pos.getZ() >> 4);
		return this.getNearestStructure(worldIn, pos);
	}
}
