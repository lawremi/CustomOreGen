package CustomOreGen.Util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.Server.DistributionSettingMap.Copyable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

public class BlockDescriptor implements Copyable<BlockDescriptor>
{
	public static class BlockInfo
    {
        private BlockState blockState;
        private boolean ignoreState;
        private CompoundTag nbt;
        
        private BlockInfo(BlockState blockState, CompoundTag nbt, boolean ignoreState) {
        	this.blockState = blockState;
        	this.nbt = nbt;
        	this.ignoreState = ignoreState;
        }
        
        public BlockInfo(BlockState blockState, CompoundTag nbt) {
	        this(blockState, nbt, false);
        }
        
        public BlockInfo(Block block, CompoundTag nbt) {
        	this(block.defaultBlockState(), nbt, true);
        }
        
        @Override
        public int hashCode()
        {
			int code = this.ignoreState ? this.blockState.getBlock().hashCode() : this.blockState.hashCode();
            if (this.nbt != null)
            	code ^= nbt.hashCode();
            return code;
        }
		
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof BlockInfo)) return false;
            BlockInfo ok = (BlockInfo)o;
            if (this.ignoreState != ok.ignoreState) return false;
            if (this.ignoreState) {
            	if (this.blockState.getBlock() != ok.blockState.getBlock()) return false;
            } else if (!this.blockState.equals(ok.blockState)) return false;
            if ((this.nbt != null || ok.nbt != null) && (this.nbt == null || !this.nbt.equals(ok.nbt))) return false;
            return true;
        }
        public BlockState getBlockState() {
        	return this.blockState;
        }
        public CompoundTag getNBT() {
			return this.nbt;
		}
        public boolean ignoresState() {
        	return this.ignoreState;
        }
        public Block getBlock() {
        	return this.blockState.getBlock();
        }
    }
	
	private static class Match
    {
		public final float weight;
		
		public Match(float weight) {
			super();
			this.weight = weight;
		}
    }
	
    protected LinkedList<Descriptor> _descriptors = new LinkedList<Descriptor>();
    protected Map<BlockInfo,Match> _matches = new Hashtable<BlockInfo,Match>();
    protected boolean _compiled = false;
    
    public BlockDescriptor()
    {
        this.clear();
    }

    public BlockDescriptor(String descriptor)
    {
        this.set(descriptor);
    }

    public BlockDescriptor(Block stone) {
		this.set(stone);
	}

	public void copyFrom(BlockDescriptor source)
    {
        this._descriptors = new LinkedList<Descriptor>(source._descriptors);
        this._matches = new Hashtable<BlockInfo, Match>(source._matches);
        this._compiled = source._compiled;
    }

	public BlockDescriptor set(Block block) {
		return this.set(ForgeRegistries.BLOCKS.getKey(block).toString());
	}
	
    public BlockDescriptor set(String descriptor)
    {
        this.clear();

        if (descriptor != null)
        {
            this.add(descriptor);
        }

        return this;
    }

    public BlockDescriptor add(String descriptor)
    {
        return this.add(descriptor, 1.0F, false, false, false, null);
    }

    public BlockDescriptor add(String descriptor, float weight, CompoundTag nbt) {
    	return this.add(descriptor, weight, false, false, false, nbt);
    }
    
    public BlockDescriptor add(String descriptor, float weight, boolean describesOre, boolean matchFirst, boolean regexp, CompoundTag nbt)
    {
        if (descriptor != null && weight != 0.0F)
        {
            this._compiled = false;
            this._descriptors.add(new Descriptor(descriptor, weight, describesOre, matchFirst, regexp, nbt));
        }

        return this;
    }

    public BlockDescriptor clear()
    {
        this._compiled = false;
        this._descriptors.clear();
        return this;
    }

    public List<Descriptor> getDescriptors()
    {
        return Collections.unmodifiableList(this._descriptors);
    }

    private void add(Block block, float weight)
    {
    	this.add(new BlockInfo(block, null), weight);
    }
    
    private void add(Block block, CompoundTag nbt, float weight)
    {
    	this.add(new BlockInfo(block, nbt), weight);
    }
    
    private void add(BlockState blockState, CompoundTag nbt, float weight)
    {
    	this.add(new BlockInfo(blockState, nbt), weight);
    }
    
    private void add(BlockInfo blockInfo, float weight)
    {
    	BlockState blockState = blockInfo.getBlockState();
    	Block block = blockState.getBlock();
    	CompoundTag nbt = blockInfo.getNBT();
    	if (nbt != null && !blockState.hasBlockEntity()) {
    		ResourceLocation name = ForgeRegistries.BLOCKS.getKey(block);
    		throw new IllegalArgumentException("NBT specified, but matching block " + 
    				name + " lacks tile entity");
    	}
        if (weight != 0.0F)
        {
            Match match = this._matches.get(blockInfo);

            if (match != null)
            {
                match = new Match(match.weight + weight);
            } else 
            {
            	match = new Match(weight);
            }

            this._matches.put(blockInfo, match);
        }
    }

	public void add(BlockDescriptor desc, float weight) {
		for (Descriptor d : desc._descriptors) {
			this.add(d.description, d.weight * weight, d.describesOre, d.matchFirst, d.regexp, d.nbt);
		}
	}

    /** The tag itself, plus its c: or forge: twin. */
    public static List<ResourceLocation> equivalentTags(ResourceLocation tag)
    {
    	String twin = tag.getNamespace().equals("forge") ? "c" : tag.getNamespace().equals("c") ? "forge" : null;
    	return twin == null ? List.of(tag) : List.of(tag, ResourceLocation.fromNamespaceAndPath(twin, tag.getPath()));
    }

    protected void compileMatches()
    {
        if (!this._compiled)
        {
            this._compiled = true;
            this._matches.clear();
            
            for (Descriptor desc : this._descriptors) {
            	if (desc.describesOre) {
            		ResourceLocation tagName = parseId(desc.description);
            		if (tagName == null) {
            			continue;
            		}
            		// forge: and c: name the same tags: Forge 1.21 is partway through moving to the shared c:
            		// namespace, and mods use either, so a tag in one also matches the blocks in the other.
            		Set<Block> tagged = new LinkedHashSet<Block>();
            		for (ResourceLocation name : equivalentTags(tagName)) {
            			ITag<Block> tag = ForgeRegistries.BLOCKS.tags().getTag(TagKey.create(ForgeRegistries.Keys.BLOCKS, name));
            			if (tag.isBound()) {
            				tag.stream().forEach(tagged::add);
            			}
            		}
            		// A tag nothing installed defines (e.g. forge:ores/zinc without a zinc mod) matches nothing,
            		// rather than failing the whole world config.
            		if(tagged.isEmpty()) {
            			warnOnce("Block tag '" + desc.description + "' is not defined by any loaded mod; it matches nothing.");
            			continue;
            		}
            		for (Block block : tagged) {
            			this.add(block, desc.weight);
            			if (desc.matchFirst) {
            				break;
            			}
            		}
            	} else if (desc.regexp) {
            		for (Block block : ForgeRegistries.BLOCKS) {
            			//desc.regexMatch compares against states
                    	float[] weights = desc.regexMatch(block);
                    	this.add(block, desc.nbt, weights[0]);
                    	if (weights[0] > 0 && desc.matchFirst) {
                    		break;
                    	}
                    	boolean matched = false;
                    	for (int m = 0; m < weights.length && !matched; ++m)
                    	{
                    		if (weights[m] > 0) {
                    			if(m == 0 )
                    				this.add(block, desc.nbt, weights[m]);
                    			else {
                                	ImmutableList<BlockState> states = block.getStateDefinition().getPossibleStates();
                                	this.add(states.get(m-1), desc.nbt, weights[m]);
                    			}
                    			if (desc.matchFirst) {
                    				matched = true;
                    			}
                    		}
                    	}
                    	if (matched) {
                    		break;
                    	}
                    }
            	} else {
            		ResourceLocation id = parseId(desc.getBlockName());
            		/*
            		 * TODO: support block state on 'desc' and construct + register here
            		 */
            		// getValue() returns air for unknown IDs, which would make every outdated ID mean "air".
            		if (id != null && ForgeRegistries.BLOCKS.containsKey(id))
            		  this.add(ForgeRegistries.BLOCKS.getValue(id), desc.nbt, desc.weight);
            		else if (id != null)
            			warnOnce("Block '" + desc.getBlockName() + "' is not registered; it matches nothing.");
            	}
            }
        }
    }

   	public Map<BlockInfo,Match> getMatches()
    {
        this.compileMatches();
        return Collections.unmodifiableMap(this._matches);
    }

    public float getWeight(BlockState blockState, CompoundTag nbt)
    {
        this.compileMatches();
        float value = 0.0F;
        Match noStateValue = this._matches.get(new BlockInfo(blockState.getBlock(), nbt));

        if (noStateValue != null)
        {
            value = noStateValue.weight;
        }

        Match stateValue = this._matches.get(new BlockInfo(blockState, nbt));

        if (stateValue != null)
        {
        	value += stateValue.weight;
        }

        return value;
    }

    public boolean matchesBlock(BlockState blockState, RandomSource rand)
    {
        float weight = this.getWeight(blockState, null);

        if (weight <= 0.0F)
        {
            return false;
        }
        else if (weight < 1.0F)
        {
            if (rand == null)
            {
                rand = RandomSource.create();
            }

            return rand.nextFloat() < weight;
        }
        else
        {
            return true;
        }
    }

    public BlockInfo getMatchingBlock(RandomSource rand)
    {
        this.compileMatches();
        float value = -1.0F;
        
        for (Entry<BlockInfo,Match> entry : _matches.entrySet()) {
        	float weight = entry.getValue().weight;
            BlockInfo info = entry.getKey();
            
            if (weight > 0.0F)
            {
                if (weight >= 1.0F)
                {
                    return info;
                }

                if (value < 0.0F)
                {
                    if (rand == null)
                    {
                        rand = RandomSource.create();
                    }

                    value = rand.nextFloat();
                }

                value -= weight;

                if (value < 0.0F)
                {
                    return info;
                }
            }
        }

        return null;
    }

    public float getTotalMatchWeight()
    {
        this.compileMatches();
        float weight = 0.0F;
        
        for (Match m : _matches.values()) {
        	if (m.weight > 0.0F)
            {
                weight += m.weight;
            }
        }

        return weight;
    }

    public String toString()
    {
        switch (this._descriptors.size())
        {
            case 0:
                return "[no blocks]";

            case 1:
                return ((Descriptor)this._descriptors.get(0)).toString();

            default:
                return this._descriptors.toString();
        }
    }

    public String[] toDetailedString()
    {
        this.compileMatches();
        String[] breakdown = new String[this._matches.size() + 1];
        breakdown[0] = this._matches.size() + " block matches";

        if (this._matches.size() > 0)
        {
            breakdown[0] += ':';
        }

        int i = 1;

        for (Entry<BlockInfo,Match> entry : _matches.entrySet()) {
        	float weight = entry.getValue().weight;
            Block block = entry.getKey().getBlock();
            
            breakdown[i] = ForgeRegistries.BLOCKS.getKey(block).toString();
            breakdown[i] += " (" + weight + ")";
            
            ++i;
        }

        return breakdown;
    }
    
    private static final Set<String> _warned = ConcurrentHashMap.newKeySet();

    private static void warnOnce(String message)
    {
    	if (_warned.add(message))
    	{
    		CustomOreGenBase.log.warn(message);
    	}
    }

    /**
     * A namespaced ID, or null (with a warning) if it isn't a valid one. Pre-1.13 configs use IDs such as
     * "Mekanism:OreBlock" or "minecraft:sand:1" (capitals, metadata) that are no longer valid.
     */
    private static ResourceLocation parseId(String id)
    {
    	ResourceLocation location = ResourceLocation.tryParse(id);
    	if (location == null)
    	{
    		warnOnce("'" + id + "' is not a valid block or tag ID (capitals and :metadata are pre-1.13); it matches nothing.");
    	}
    	return location;
    }

    private class Descriptor
    {
        public final String description;
        public final float weight;
        public final boolean describesOre;
        public final boolean matchFirst;
        public final boolean regexp;
        public final CompoundTag nbt;
        private Pattern pattern = null;
        
        public Descriptor(String description, float weight, boolean describesOre, boolean matchFirst, boolean regexp, CompoundTag nbt)
        {
            this.description = description;
            this.weight = weight;
			this.describesOre = describesOre;
			this.matchFirst = matchFirst;
			this.regexp = regexp;
			this.nbt = nbt;
        }

        public Pattern getPattern()
        {
            if (this.pattern == null)
            {
                this.pattern = Pattern.compile(this.description, Pattern.CASE_INSENSITIVE);
            }

            return this.pattern;
        }
        
        public String getBlockName() {
        	boolean hasMeta = this.description.indexOf(':') != this.description.lastIndexOf(':');
        	if (hasMeta) {
        		return this.description.substring(0, this.description.lastIndexOf(':'));
        	} else {
        		return this.description;
        	}
        }
        
        public float[] regexMatch(Block block)
        {
            float[] weights = new float[1];
            String name = ForgeRegistries.BLOCKS.getKey(block).toString();
        	
            if (!this.getPattern().matcher(name).matches())
            {
            	ImmutableList<BlockState> states = block.getStateDefinition().getPossibleStates();
                weights = new float[states.size() + 1];
                int m=1;
            	for (BlockState state : states)
            	{
            		//TODO: this might be improved
            		if (this.getPattern().matcher(state.toString()).matches())
            		{
            			weights[m] += this.weight;
            			m++;
            		}
            	}
            }
            else
            {
            	weights[0] += this.weight;
            }
            return weights;
        }
        
        public String toString()
        {
            return this.description + " - " + Float.toString(this.weight);
        }
    }

	public boolean isEmpty() {
		return this._descriptors.isEmpty();
	}
}
