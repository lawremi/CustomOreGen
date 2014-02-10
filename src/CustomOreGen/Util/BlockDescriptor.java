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

import cpw.mods.fml.common.FMLLog;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import CustomOreGen.Server.DistributionSettingMap.Copyable;

public class BlockDescriptor implements Copyable<BlockDescriptor>
{
    protected LinkedList<Descriptor> _descriptors = new LinkedList();
    protected Map<Integer,Float> _matches = new Hashtable();
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

    public void copyFrom(BlockDescriptor source)
    {
        this._descriptors = new LinkedList(source._descriptors);
        this._matches = new Hashtable(source._matches);
        this._compiled = source._compiled;
        this._fastMatch = (float[])source._fastMatch.clone();
    }

    public BlockDescriptor set(String descriptor)
    {
        this.clear();

        if (descriptor != null)
        {
            this._descriptors.add(new Descriptor(descriptor, 1.0F, false));
        }

        return this;
    }

    public BlockDescriptor add(String descriptor)
    {
        return this.add(descriptor, 1.0F, false);
    }

    public BlockDescriptor add(String descriptor, float weight) {
    	return this.add(descriptor, weight, false);
    }
    
    public BlockDescriptor add(String descriptor, float weight, boolean describesOre)
    {
        if (descriptor != null && weight != 0.0F)
        {
            this._compiled = false;
            this._descriptors.add(new Descriptor(descriptor, weight, describesOre));
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

    protected void add(int blockID, int metaData, float weight)
    {
        if (weight != 0.0F)
        {
            Integer key = Integer.valueOf(blockID << Short.SIZE | metaData & Short.MAX_VALUE);
            Float currentValue = this._matches.get(key);

            if (currentValue != null)
            {
                weight += currentValue;
            }

            this._matches.put(key, weight);

            if (blockID >= 0 && blockID < this._fastMatch.length)
            {
                if (metaData == OreDictionary.WILDCARD_VALUE && !Float.isNaN(this._fastMatch[blockID]))
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

    protected float[] regexMatch(String id, String name, boolean describesOre)
    {
        float[] weights = new float[Short.SIZE + 1];
        
        for (Descriptor desc : this._descriptors) {
        	if (desc.describesOre != describesOre)
        		continue;
        	if ((id == null || !desc.getPattern().matcher(id).matches()) && (name == null || !desc.getPattern().matcher(name).matches()))
            {
                for (int m = 0; m < Short.SIZE; ++m)
                {
                    if (id != null && desc.getPattern().matcher(id + ":" + m).matches() || name != null && desc.getPattern().matcher(name + ":" + m).matches())
                    {
                        ++desc.matches;
                        weights[m] += desc.weight;
                    }
                }
            }
            else
            {
                ++desc.matches;
                weights[Short.SIZE] += desc.weight;
            }
        }

        return weights;
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
            }
            
            float[] var10 = this.regexMatch("0", "air", false);
            this.add(0, OreDictionary.WILDCARD_VALUE, var10[Short.SIZE]);
            
            for (Block block : Block.blocksList) {
            	if (block != null && block.blockID != 0)
                {
                    String id = Integer.toString(block.blockID);
                    String name = block.getUnlocalizedName() == null ? null : block.getUnlocalizedName().replace("tile.", "");
                    float[] weights = this.regexMatch(id, name, false);
                    this.add(block.blockID, OreDictionary.WILDCARD_VALUE, weights[Short.SIZE]);

                    for (int m = 0; m < Short.SIZE; ++m)
                    {
                        this.add(block.blockID, m, weights[m]);
                    }
                }
            }
            
            for (String oreName : OreDictionary.getOreNames()) {
            	float[] weights = this.regexMatch(Integer.toString(OreDictionary.getOreID(oreName)), oreName, true);
            	for (ItemStack ore : OreDictionary.getOres(oreName)) {
            		boolean isBlock = ore.itemID < Block.blocksList.length;
            		if (isBlock) {
            			this.add(ore.itemID, ore.getItemDamage(), weights[Short.SIZE]);
                	}
            	}
            }
        }
    }

    public Map getMatches()
    {
        this.compileMatches();
        return Collections.unmodifiableMap(this._matches);
    }

    public float getWeight_fast(int blockID)
    {
        this.compileMatches();
        return blockID >= 0 && blockID < this._fastMatch.length ? this._fastMatch[blockID] : Float.NaN;
    }

    public float getWeight(int blockID, int metaData)
    {
        this.compileMatches();
        float value = 0.0F;
        Float noMetaValue = (Float)this._matches.get(Integer.valueOf(blockID << Short.SIZE | OreDictionary.WILDCARD_VALUE));

        if (noMetaValue != null)
        {
            value = noMetaValue.floatValue();
        }

        if (metaData >= 0)
        {
            Float metaValue = (Float)this._matches.get(Integer.valueOf(blockID << Short.SIZE | metaData & Short.MAX_VALUE));

            if (metaValue != null)
            {
                value += metaValue.floatValue();
            }
        }

        return value;
    }

    public int matchesBlock_fast(int blockID)
    {
        float weight = this.getWeight_fast(blockID);
        return Float.isNaN(weight) ? -1 : (weight <= 0.0F ? 0 : (weight < 1.0F ? -1 : 1));
    }

    public boolean matchesBlock(int blockID, int metaData, Random rand)
    {
        float weight = this.getWeight(blockID, metaData);

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

    public int getMatchingBlock(Random rand)
    {
        this.compileMatches();
        float value = -1.0F;
        
        for (Entry<Integer,Float> entry : _matches.entrySet()) {
        	float weight = entry.getValue();
            int blockID = entry.getKey() >>> Short.SIZE;
            int metaData = entry.getKey() & Short.MAX_VALUE;

            if (metaData >= Short.MAX_VALUE)
            {
                metaData = 0;
            }

            if (weight > 0.0F)
            {
                if (weight >= 1.0F)
                {
                    return blockID << Short.SIZE | metaData;
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
                    return blockID << Short.SIZE | metaData;
                }
            }
        }

        return -1;
    }

    public float getTotalMatchWeight()
    {
        this.compileMatches();
        float weight = 0.0F;
        
        for (float val : _matches.values()) {
        	if (val > 0.0F)
            {
                weight += val;
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
            breakdown[0] = breakdown[0] + ':';
        }

        int i = 1;

        for (Entry<Integer,Float> entry : _matches.entrySet()) {
        	float weight = entry.getValue();
            int blockID = entry.getKey() >>> Short.SIZE;
            int metaData = entry.getKey() & Short.MAX_VALUE;
            Block block = Block.blocksList[blockID];

            if (block == null)
            {
                breakdown[i] = blockID == 0 ? "Air" : "[??]";
            }
            else
            {
                breakdown[i] = block.getLocalizedName();
            }

            breakdown[i] = breakdown[i] + " (" + blockID;

            if (metaData != OreDictionary.WILDCARD_VALUE)
            {
                breakdown[i] = breakdown[i] + ":" + metaData;
            }

            breakdown[i] = breakdown[i] + ") - " + weight;
            
            ++i;
        }

        return breakdown;
    }
    
    private class Descriptor
    {
        public final String description;
        public final float weight;
        public final boolean describesOre;
        public int matches = 0;
        private Pattern pattern = null;

        public Descriptor(String description, float weight, boolean describesOre)
        {
            this.description = description;
            this.weight = weight;
			this.describesOre = describesOre;
        }

        public Pattern getPattern()
        {
            if (this.pattern == null)
            {
                this.pattern = Pattern.compile(this.description, 2);
            }

            return this.pattern;
        }

        public String toString()
        {
            return this.description + " - " + Float.toString(this.weight);
        }
    }

}
