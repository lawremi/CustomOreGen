package CustomOreGen.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

import CustomOreGen.Server.DistributionSettingMap.Copyable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

public class BlockDescriptor implements Copyable<BlockDescriptor>
{
	public static class BlockInfo
    {
        private IBlockState blockState;
        private boolean wildcard;
        private NBTTagCompound nbt;
        
        private BlockInfo(IBlockState blockState, NBTTagCompound nbt, boolean wildcard) {
        	this.blockState = blockState;
        	this.nbt = nbt;
        	this.wildcard = wildcard;
        }
        
        public BlockInfo(IBlockState blockState, NBTTagCompound nbt) {
	        this(blockState, nbt, false);
        }
        
        public BlockInfo(Block block, int metadata, NBTTagCompound nbt) {
        	this(metadata == OreDictionary.WILDCARD_VALUE ? block.getDefaultState() : block.getStateFromMeta(metadata),
         	     nbt, metadata == OreDictionary.WILDCARD_VALUE);
        }
        
        public BlockInfo(Block block, NBTTagCompound nbt) {
        	this(block.getDefaultState(), nbt, true);
        }
        
        @Override
        public int hashCode()
        {
			int code = this.wildcard ? Block.getIdFromBlock(this.blockState.getBlock()) : this.blockState.hashCode();
            if (this.nbt != null)
            	code ^= nbt.hashCode();
            return code;
        }
		
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof BlockInfo)) return false;
            BlockInfo ok = (BlockInfo)o;
            if (!this.blockState.equals(ok.blockState)) return false;
            if ((this.nbt != null || ok.nbt != null) && (this.nbt == null || !this.nbt.equals(ok.nbt))) return false;
            return true;
        }
        public IBlockState getBlockState() {
        	return this.blockState;
        }
        public NBTTagCompound getNBT() {
			return this.nbt;
		}
        public boolean isWildcard() {
        	return this.wildcard;
        }
        public Block getBlock() {
        	return this.blockState.getBlock();
        }
        public int getMetadata() {
        	return this.getBlock().getMetaFromState(this.blockState);
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
    protected float[] _fastMatch = new float[256];

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
        this._fastMatch = (float[])source._fastMatch.clone();
    }

	public BlockDescriptor set(Block block) {
		return this.set(Block.REGISTRY.getNameForObject(block).toString());
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

    public BlockDescriptor add(String descriptor, float weight, NBTTagCompound nbt) {
    	return this.add(descriptor, weight, false, false, false, nbt);
    }
    
    public BlockDescriptor add(String descriptor, float weight, boolean describesOre, boolean matchFirst, boolean regexp, NBTTagCompound nbt)
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

    private void add(Block block, NBTTagCompound nbt, float weight)
    {
    	this.add(new BlockInfo(block, nbt), weight);
    }
    
    private void add(Block block, int metadata, NBTTagCompound nbt, float weight)
    {
    	this.add(new BlockInfo(block, metadata, nbt), weight);
    }
    
    private void add(BlockInfo blockInfo, float weight)
    {
    	IBlockState blockState = blockInfo.getBlockState();
    	Block block = blockState.getBlock();
    	NBTTagCompound nbt = blockInfo.getNBT();
    	if (nbt != null && !block.hasTileEntity(blockState)) {
    		throw new IllegalArgumentException("NBT specified, but matching block " + 
    				block.getUnlocalizedName() + " lacks tile entity");
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

            int blockID = Block.getIdFromBlock(block);
            if (blockID >= 0 && blockID < this._fastMatch.length)
            {
                if (blockInfo.isWildcard() && !Float.isNaN(this._fastMatch[blockID]))
                {
                    this._fastMatch[blockID] += weight;
                }
                else
                {
                    this._fastMatch[blockID] = Float.NaN;
                }
            }
        }
    }

	public void add(BlockDescriptor desc, float weight) {
		for (Descriptor d : desc._descriptors) {
			this.add(d.description, d.weight * weight, d.describesOre, d.matchFirst, d.regexp, d.nbt);
		}
	}

    protected void compileMatches()
    {
        if (!this._compiled)
        {
            this._compiled = true;
            this._matches.clear();
            Arrays.fill(this._fastMatch, 0.0F);
            
            for (Descriptor desc : this._descriptors) {
            	if (desc.describesOre) {
            		for (ItemStack ore : OreDictionary.getOres(desc.description)) {
            			Item oreItem = ore.getItem();
            			if (oreItem instanceof ItemBlock) {
            				Block block = ((ItemBlock)oreItem).block;
            				int damage = ore.getItemDamage();
            				NBTTagCompound nbt = ore.getTagCompound();
            				if (nbt == null) {
            					nbt = TileEntityHelper.tryToCreateGTPrefixBlockNBT(ore);
            					if (nbt != null) {
            						block = Block.getBlockFromName("gregtech:gt.meta.ore.normal.default");
            						damage = 2; // mining level stone
            					}
            				}
            				// FIXME: Blocks tend to be registered as meta 0, even when the meta is irrelevant,
            				// so we are unable to take advantage of the fast ID hash. 
            				// This is particularly true of vanilla 'stone'.
            				this.add(block, damage, nbt, desc.weight);
            			}
            			if (desc.matchFirst) {
            				break;
            			}
            		}            		
            	} else if (desc.regexp) {
            		for (Block block : Block.REGISTRY) {
                    	String name = Block.REGISTRY.getNameForObject(block).toString();
                    	float[] weights = desc.regexMatch(name);
                    	this.add(block, desc.nbt, weights[Short.SIZE]);
                    	if (weights[Short.SIZE] > 0 && desc.matchFirst) {
                    		break;
                    	}
                    	boolean matched = false;
                    	for (int m = 0; m < Short.SIZE && !matched; ++m)
                    	{
                    		this.add(block, m, desc.nbt, weights[m]);
                    		if (weights[m] > 0 && desc.matchFirst) {
                    			matched = true;
                    		}
                    	}
                    	if (matched) {
                    		break;
                    	}
                    }
            	} else {
            		Block block = Block.getBlockFromName(desc.getBlockName());
            		if (block != null)
            		  this.add(block, desc.getMeta(), desc.nbt, desc.weight);
            	}
            }   
        }
    }

   	public Map<BlockInfo,Match> getMatches()
    {
        this.compileMatches();
        return Collections.unmodifiableMap(this._matches);
    }

    public float getWeight_fast(IBlockState blockState)
    {
        this.compileMatches();
        int blockID = Block.getIdFromBlock(blockState.getBlock());
        return blockID >= 0 && blockID < this._fastMatch.length ? this._fastMatch[blockID] : Float.NaN;
    }

    public float getWeight(IBlockState blockState, NBTTagCompound nbt)
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

    public int matchesBlock_fast(IBlockState block)
    {
        float weight = this.getWeight_fast(block);
        return Float.isNaN(weight) ? -1 : (weight <= 0.0F ? 0 : (weight < 1.0F ? -1 : 1));
    }
    
    public boolean matchesBlock(IBlockState blockState, Random rand)
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
                rand = new Random();
            }

            return rand.nextFloat() < weight;
        }
        else
        {
            return true;
        }
    }

    public BlockInfo getMatchingBlock(Random rand)
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
                        rand = new Random();
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
            int metaData = entry.getKey().getMetadata();
            Block block = entry.getKey().getBlock();
            
            breakdown[i] = Block.REGISTRY.getNameForObject(block).toString();
            
            if (metaData != OreDictionary.WILDCARD_VALUE)
            {
                breakdown[i] += ":" + metaData;
            }

            breakdown[i] += " (" + weight + ")";
            
            ++i;
        }

        return breakdown;
    }
    
    private class Descriptor
    {
        public final String description;
        public final float weight;
        public final boolean describesOre;
        public final boolean matchFirst;
        public final boolean regexp;
        public final NBTTagCompound nbt;
        private Pattern pattern = null;
        
        public Descriptor(String description, float weight, boolean describesOre, boolean matchFirst, boolean regexp, NBTTagCompound nbt)
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
        
        public int getMeta() {
        	boolean hasMeta = this.getBlockName().length() < this.description.length();
        	if (hasMeta) {
        		return Integer.valueOf(this.description.substring(this.getBlockName().length() + 1));
        	} else {
        		return OreDictionary.WILDCARD_VALUE;
        	}
        }

        public float[] regexMatch(String name)
        {
            float[] weights = new float[Short.SIZE + 1];
            
            if (!this.getPattern().matcher(name).matches())
            {
            	for (int m = 0; m < Short.SIZE; ++m)
            	{
            		if (this.getPattern().matcher(name + ":" + m).matches())
            		{
            			weights[m] += this.weight;
            		}
            	}
            }
            else
            {
            	weights[Short.SIZE] += this.weight;
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
