package CustomOreGen.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import CustomOreGen.Server.DistributionSettingMap.Copyable;

public class BiomeDescriptor implements Copyable<BiomeDescriptor>
{
    protected LinkedList<Descriptor> _descriptors = new LinkedList();
    protected Map<BiomeGenBase,Float> _matches = new Hashtable();
    protected boolean _compiled = false;
    
    private String name;

    public BiomeDescriptor()
    {
        this.clear();
    }

    public BiomeDescriptor(String descriptor)
    {
        this.set(descriptor);
    }
    
    public void copyFrom(BiomeDescriptor source)
    {
        this._descriptors = new LinkedList(source._descriptors);
        this._matches = new Hashtable(source._matches);
        this._compiled = source._compiled;
    }
    
    public String getName() {
    	return name;
    }
    
    public void setName(String name) {
    	this.name = name;
    }
    
    public BiomeDescriptor set(String descriptor)
    {
        this.clear();

        if (descriptor != null)
        {
            this.add(descriptor);
        }

        return this;
    }

    public BiomeDescriptor add(String descriptor)
    {
        return this.add(descriptor, 1.0F);
    }

    public BiomeDescriptor add(String descriptor, float weight)
    {
        return this.add(descriptor, 1.0F, new Climate(), false);
    }
    
    public BiomeDescriptor add(String descriptor, float weight, Climate climate, boolean describesType)
    {
        if (descriptor != null && weight != 0.0F)
        {
            this._compiled = false;
            this._descriptors.add(new Descriptor(descriptor, weight, climate, describesType));
        }

        return this;
    }
    
    public BiomeDescriptor addAll(BiomeDescriptor descriptor, float weight) {
    	this._compiled = false;
    	if (weight == 1.0F) {
    		this._descriptors.addAll(descriptor._descriptors);
    	} else {
    		for (Descriptor desc : descriptor._descriptors) {
    			add(desc.description, desc.weight * weight, desc.climate, desc.describesType);
    		}
    	}
    	return this;
	}

    public BiomeDescriptor clear()
    {
        this._compiled = false;
        this._descriptors.clear();
        return this;
    }

    public List getDescriptors()
    {
        return Collections.unmodifiableList(this._descriptors);
    }

    protected void add(BiomeGenBase biome, float weight)
    {
        if (biome != null && weight != 0.0F)
        {
            Float currentValue = (Float)this._matches.get(biome);

            if (currentValue != null)
            {
                weight += currentValue.floatValue();
            }

            this._matches.put(biome, Float.valueOf(weight));
        }
    }

    protected float matchingWeight(BiomeGenBase biome)
    {
        float totalWeight = 0.0F;
        
        String id = Integer.toString(biome.biomeID);
        String name = biome.biomeName;
        BiomeDictionary.Type[] types = BiomeDictionary.getTypesForBiome(biome);
        
        for (Descriptor desc : this._descriptors) {
            int oldMatches = desc.matches;
            Matcher matcher;
            
            if (!desc.climate.isCompatible(biome))
            	continue;
            
            if (desc.describesType) {
            	for (BiomeDictionary.Type type : types) {	
            		matcher = desc.getPattern().matcher(type.name());
            	
            		if (matcher.matches())
            		{
            			++desc.matches;
            			totalWeight += desc.weight;
            			break;
            		}
            	}
            } else {
            	if (desc.matches == oldMatches && id != null)
            	{
            		matcher = desc.getPattern().matcher(id);

            		if (matcher.matches())
            		{
            			++desc.matches;
            			totalWeight += desc.weight;
            		}
            	}

            	if (desc.matches == oldMatches && name != null)
            	{
            		matcher = desc.getPattern().matcher(name);

            		if (matcher.matches())
            		{
            			++desc.matches;
            			totalWeight += desc.weight;
            		}
            	}
            }
        }
        return totalWeight;
    }

    protected void compileMatches()
    {
        if (!this._compiled)
        {
            this._compiled = true;
            this._matches.clear();
            
            for (Descriptor desc : this._descriptors) {
            	desc.matches = 0;
            }
            	
            for (BiomeGenBase biome : BiomeGenBase.biomeList) {
                if (biome != null)
                {
                    this.add(biome, this.matchingWeight(biome));
                }            	
            }
        }
    }

    public Map getMatches()
    {
        this.compileMatches();
        return Collections.unmodifiableMap(this._matches);
    }

    public float getWeight(BiomeGenBase biome)
    {
        this.compileMatches();
        Float value = (Float)this._matches.get(biome);
        return value == null ? 0.0F : value.floatValue();
    }

    public boolean matchesBiome(BiomeGenBase biome, Random rand)
    {
        float weight = this.getWeight(biome);

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

    public BiomeGenBase getMatchingBiome(Random rand)
    {
        this.compileMatches();
        float value = -1.0F;
        
        for (Entry<BiomeGenBase,Float> entry : this._matches.entrySet()) {
        	float weight = entry.getValue();
            BiomeGenBase biome = entry.getKey();

            if (weight > 0.0F)
            {
                if (weight >= 1.0F)
                {
                    return biome;
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
                    return biome;
                }
            }

        }
        return null;
    }

    public float getTotalMatchWeight()
    {
        this.compileMatches();
        float weight = 0.0F;
        
        for (Float val : this._matches.values()) {
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
                return "[no biomes]";

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
        breakdown[0] = this._matches.size() + " biome matches";

        if (this._matches.size() > 0)
        {
            breakdown[0] = breakdown[0] + ':';
        }

        int i = 1;

        for (Entry<BiomeGenBase,Float> entry : this._matches.entrySet()) {
        	float weight = entry.getValue();
            BiomeGenBase biome = entry.getKey();

            if (biome == null)
            {
                breakdown[i] = "[??]";
            }
            else
            {
                breakdown[i] = biome.biomeName;
            }

            breakdown[i] = breakdown[i] + " - " + weight;
            ++i;
        }
        
        return breakdown;
    }

    private static class Descriptor
    {
        public final String description;
        public final float weight;
        public final Climate climate;
        public final boolean describesType;
        public int matches = 0;
        private Pattern pattern = null;

        public Descriptor(String description, float weight, Climate climate, boolean describesType)
        {
            this.description = description;
            this.weight = weight;
			this.climate = climate;
            this.describesType = describesType;
        }

        public Pattern getPattern()
        {
            if (this.pattern == null)
            {
                this.pattern = Pattern.compile(this.description, Pattern.CASE_INSENSITIVE);
            }

            return this.pattern;
        }

        public String toString()
        {
            return this.description + " - " + Float.toString(this.weight);
        }
    }
    
    public static class Climate {
    	public final float minTemperature, maxTemperature;
        public final float minRainfall, maxRainfall;
        
        public Climate(float minTemperature, float maxTemperature, float minRainfall, float maxRainfall) {
        	this.minTemperature = minTemperature;
			this.maxTemperature = maxTemperature;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
        }
        
        public Climate() {
        	this.minTemperature = this.minRainfall = 0F;
			this.maxTemperature = this.maxRainfall = 2.0F;
        }
        
        public boolean isCompatible(BiomeGenBase biome) {
			return biome.temperature >= minTemperature && biome.temperature <= maxTemperature &&
				   biome.rainfall >= minRainfall && biome.rainfall <= maxRainfall;
		}
    }

}
