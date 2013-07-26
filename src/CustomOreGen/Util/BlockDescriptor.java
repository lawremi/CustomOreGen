package CustomOreGen.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
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
            this._descriptors.add(new Descriptor(descriptor, 1.0F));
        }

        return this;
    }

    public BlockDescriptor add(String descriptor)
    {
        return this.add(descriptor, 1.0F);
    }

    public BlockDescriptor add(String descriptor, float weight)
    {
        if (descriptor != null && weight != 0.0F)
        {
            this._compiled = false;
            this._descriptors.add(new Descriptor(descriptor, weight));
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
            Integer key = Integer.valueOf(blockID << 16 | metaData & 65535);
            Float currentValue = (Float)this._matches.get(key);

            if (currentValue != null)
            {
                weight += currentValue.floatValue();
            }

            this._matches.put(key, Float.valueOf(weight));

            if (blockID >= 0 && blockID < this._fastMatch.length)
            {
                if (metaData == -1 && !Float.isNaN(this._fastMatch[blockID]))
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

    protected float[] regexMatch(String id, String name)
    {
        float[] weights = new float[17];
        
        for (Descriptor desc : this._descriptors) {
        	if ((id == null || !desc.getPattern().matcher(id).matches()) && (name == null || !desc.getPattern().matcher(name).matches()))
            {
                for (int m = 0; m < 16; ++m)
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
                weights[16] += desc.weight;
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
            
            float[] var10 = this.regexMatch("0", "air");
            this.add(0, -1, var10[16]);
            
            for (Block block : Block.blocksList) {
            	if (block != null && block.blockID != 0)
                {
                    String id = Integer.toString(block.blockID);
                    String name = block.getUnlocalizedName() == null ? null : block.getUnlocalizedName().replace("tile.", "");
                    float[] weights = this.regexMatch(id, name);
                    this.add(block.blockID, -1, weights[16]);

                    for (int m = 0; m < 16; ++m)
                    {
                        this.add(block.blockID, m, weights[m]);
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
        Float noMetaValue = (Float)this._matches.get(Integer.valueOf(blockID << 16 | 65535));

        if (noMetaValue != null)
        {
            value = noMetaValue.floatValue();
        }

        if (metaData >= 0)
        {
            Float metaValue = (Float)this._matches.get(Integer.valueOf(blockID << 16 | metaData & 65535));

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
            int blockID = entry.getKey() >>> 16;
            int metaData = entry.getKey() & 65535;

            if (metaData >= 32768)
            {
                metaData = 0;
            }

            if (weight > 0.0F)
            {
                if (weight >= 1.0F)
                {
                    return blockID << 16 | metaData;
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
                    return blockID << 16 | metaData;
                }
            }
        }

        return -1;
    }

    public float getTotalMatchWeight()
    {
        this.compileMatches();
        float weight = 0.0F;
        
        for (Float val : _matches.values()) {
        	if (val.floatValue() > 0.0F)
            {
                weight += val.floatValue();
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
            int blockID = entry.getKey() >>> 16;
            int metaData = entry.getKey() & 65535;
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

            if (metaData < 32768)
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
        public int matches = 0;
        private Pattern pattern = null;

        public Descriptor(String description, float weight)
        {
            this.description = description;
            this.weight = weight;
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
