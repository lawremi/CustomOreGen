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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import CustomOreGen.Server.DistributionSettingMap.Copyable;
import cpw.mods.fml.common.registry.GameData;

public class BlockDescriptor implements Copyable<BlockDescriptor>
{
	public static class BlockInfo
    {
        private Block block;
        private int metadata;
        private NBTTagCompound nbt;
        
        public BlockInfo(Block block, int metadata, NBTTagCompound nbt) {
			super();
			this.block = block;
			this.metadata = metadata;
			this.nbt = nbt;
		}
        
		@Override
        public int hashCode()
        {
            int code = Block.getIdFromBlock(this.block) << Short.SIZE | this.metadata & Short.MAX_VALUE;
            if (this.nbt != null)
            	code ^= nbt.hashCode();
            return code;
        }
		
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof BlockInfo)) return false;
            BlockInfo ok = (BlockInfo)o;
            if (this.block != ok.block) return false;
            if (this.metadata != ok.metadata) return false;
            if ((this.nbt != null || ok.nbt != null) && (this.nbt == null || !this.nbt.equals(ok.nbt))) return false;
            return true;
        }
        public int getMetadata() {
        	return this.metadata;
        }
        public Block getBlock() {
        	return this.block;
        }
		public NBTTagCompound getNBT() {
			return this.nbt;
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
	
    protected LinkedList<Descriptor> _descriptors = new LinkedList();
    protected Map<BlockInfo,Match> _matches = new Hashtable();
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
        this._descriptors = new LinkedList(source._descriptors);
        this._matches = new Hashtable(source._matches);
        this._compiled = source._compiled;
        this._fastMatch = (float[])source._fastMatch.clone();
    }

	public BlockDescriptor set(Block block) {
		return this.set(Block.blockRegistry.getNameForObject(block));
	}
	
    public BlockDescriptor set(String descriptor)
    {
        this.clear();

        if (descriptor != null)
        {
            this._descriptors.add(new Descriptor(descriptor, 1.0F, false, false, null));
        }

        return this;
    }

    public BlockDescriptor add(String descriptor)
    {
        return this.add(descriptor, 1.0F, false, false, null);
    }

    public BlockDescriptor add(String descriptor, float weight, NBTTagCompound nbt) {
    	return this.add(descriptor, weight, false, false, nbt);
    }
    
    public BlockDescriptor add(String descriptor, float weight, boolean describesOre, boolean regexp, NBTTagCompound nbt)
    {
        if (descriptor != null && weight != 0.0F)
        {
            this._compiled = false;
            this._descriptors.add(new Descriptor(descriptor, weight, describesOre, regexp, nbt));
        }

        return this;
    }

    public BlockDescriptor clear()
    {
        this._compiled = false;
        this._descriptors.clear();
        return this;
    }

    public List getDescriptors()
    {
        return Collections.unmodifiableList(this._descriptors);
    }

    private void add(Block block, int metadata, NBTTagCompound nbt, float weight)
    {
    	if (nbt != null && !block.hasTileEntity(metadata)) {
    		throw new IllegalArgumentException("NBT specified, but matching block " + 
    				block.getUnlocalizedName() + ":" + metadata + " lacks tile entity");
    	}
        if (weight != 0.0F)
        {
            BlockInfo key = new BlockInfo(block, metadata, nbt);
            Match match = this._matches.get(key);

            if (match != null)
            {
                match = new Match(match.weight + weight);
            } else 
            {
            	match = new Match(weight);
            }

            this._matches.put(key, match);

            int blockID = Block.getIdFromBlock(block);
            if (blockID >= 0 && blockID < this._fastMatch.length)
            {
                if (metadata == OreDictionary.WILDCARD_VALUE && !Float.isNaN(this._fastMatch[blockID]))
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
			this.add(d.description, d.weight * weight, d.describesOre, d.regexp, d.nbt);
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
            	desc.matches = 0;
            	if (desc.describesOre) {
            		for (ItemStack ore : OreDictionary.getOres(desc.description)) {
            			Item oreItem = ore.getItem();
            			if (oreItem instanceof ItemBlock) {
            				Block oreBlock = ((ItemBlock)oreItem).field_150939_a;
            				// FIXME: Blocks tend to be registered as meta 0, even when the meta is irrelevant,
            				// so we are unable to take advantage of the fast ID hash. 
            				// This is particularly true of vanilla 'stone'.
            				this.add(oreBlock, ore.getItemDamage(), desc.nbt, desc.weight);
            			}
            		}            		
            	} else if (desc.regexp) {
            		for (Block block : (Iterable<Block>)GameData.getBlockRegistry()) {
                    	String name = Block.blockRegistry.getNameForObject(block);
                    	float[] weights = desc.regexMatch(name);
                    	this.add(block, OreDictionary.WILDCARD_VALUE, desc.nbt, weights[Short.SIZE]);	
                    	
                    	for (int m = 0; m < Short.SIZE; ++m)
                    	{
                    		this.add(block, m, desc.nbt, weights[m]);
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

   	public Map getMatches()
    {
        this.compileMatches();
        return Collections.unmodifiableMap(this._matches);
    }

    public float getWeight_fast(Block block)
    {
        this.compileMatches();
        int blockID = Block.getIdFromBlock(block);
        return blockID >= 0 && blockID < this._fastMatch.length ? this._fastMatch[blockID] : Float.NaN;
    }

    public float getWeight(Block block, int metaData, NBTTagCompound nbt)
    {
        this.compileMatches();
        float value = 0.0F;
        Match noMetaValue = this._matches.get(new BlockInfo(block, OreDictionary.WILDCARD_VALUE, nbt));

        if (noMetaValue != null)
        {
            value = noMetaValue.weight;
        }

        if (metaData != OreDictionary.WILDCARD_VALUE)
        {
            Match metaValue = this._matches.get(new BlockInfo(block, metaData, nbt));

            if (metaValue != null)
            {
                value += metaValue.weight;
            }
        }

        return value;
    }

    public int matchesBlock_fast(Block block)
    {
        float weight = this.getWeight_fast(block);
        return Float.isNaN(weight) ? -1 : (weight <= 0.0F ? 0 : (weight < 1.0F ? -1 : 1));
    }

    public boolean matchesBlock(Block block, int metaData, Random rand)
    {
        float weight = this.getWeight(block, metaData, null);

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
            
            if (info.getMetadata() == OreDictionary.WILDCARD_VALUE)
            {
                info = new BlockInfo(info.getBlock(), 0, info.getNBT());
            }
			
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
            
            breakdown[i] = Block.blockRegistry.getNameForObject(block);
            
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
        public final boolean regexp;
        public final NBTTagCompound nbt;
        public int matches = -1;
        private Pattern pattern = null;
        
        public Descriptor(String description, float weight, boolean describesOre, boolean regexp, NBTTagCompound nbt)
        {
            this.description = description;
            this.weight = weight;
			this.describesOre = describesOre;
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
            			++this.matches;
            			weights[m] += this.weight;
            		}
            	}
            }
            else
            {
            	++this.matches;
            	weights[Short.SIZE] += this.weight;
            }
            return weights;
        }
        
        public String toString()
        {
            return this.description + " - " + Float.toString(this.weight);
        }
    }
}
