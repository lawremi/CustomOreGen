package CustomOreGen.Util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import CustomOreGen.Server.DistributionSettingMap.Copyable;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

public class BiomeDescriptor implements Copyable<BiomeDescriptor>
{
    protected LinkedList<Descriptor> _descriptors = new LinkedList<Descriptor>();
    protected Map<Integer,Float> _matches = new Hashtable<Integer, Float>();
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
        this._descriptors = new LinkedList<Descriptor>(source._descriptors);
        this._matches = new Hashtable<Integer,Float>(source._matches);
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
        return this.add(descriptor, 1.0F, new BiomeRestriction(), false);
    }
    
    public BiomeDescriptor add(String descriptor, float weight, BiomeRestriction climate, boolean describesType)
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

    public List<Descriptor> getDescriptors()
    {
        return Collections.unmodifiableList(this._descriptors);
    }

    protected void add(Biome biome, float weight)
    {
        if (biome != null && weight != 0.0F)
        {
            Float currentValue = (Float)this._matches.get(Biome.getIdForBiome(biome));

            if (currentValue != null)
            {
                weight += currentValue.floatValue();
            }

            this._matches.put(Biome.getIdForBiome(biome), Float.valueOf(weight));
        }
    }

    protected float matchingWeight(Biome biome)
    {
        float totalWeight = 0.0F;
        
        String name = biome.getBiomeName();
        
        for (Descriptor desc : this._descriptors) {
            Matcher matcher;
            if (!desc.climate.isCompatible(biome))
            	continue;
            
            if (desc.describesType) {
            	BiomeDictionary.Type type = BiomeDictionary.Type.valueOf(desc.description.toUpperCase());
            	// instead of this, because we do not want to add a new type if it does not exist:
            	//BiomeDictionary.Type type = BiomeDictionary.Type.getType(desc.description);
            	if (BiomeDictionary.isBiomeOfType(biome, type))
            	{
            		totalWeight += desc.weight;
            	}
            } else {
            	if (name != null)
            	{
            		matcher = desc.getPattern().matcher(name);

            		if (matcher.matches())
            		{
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
            
            for (Biome biome : Biome.REGISTRY) {
                if (biome != null)
                {
                	this.add(biome, this.matchingWeight(biome));
                }
            }
        }
    }
    
    public float getWeight(Biome biome)
    {
        this.compileMatches();
        Float value = (Float)this._matches.get(Biome.getIdForBiome(biome));
        return value == null ? 0.0F : value.floatValue();
    }

    public boolean matchesBiome(Biome biome, Random rand)
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

    public Biome getMatchingBiome(Random rand)
    {
        this.compileMatches();
        float value = -1.0F;
        
        for (Entry<Integer,Float> entry : this._matches.entrySet()) {
        	float weight = entry.getValue();
            Biome biome = Biome.getBiome(entry.getKey());

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

        for (Entry<Integer,Float> entry : this._matches.entrySet()) {
        	float weight = entry.getValue();
            Biome biome = Biome.getBiome(entry.getKey());

            if (biome == null)
            {
                breakdown[i] = "[??]";
            }
            else
            {
                breakdown[i] = biome.getBiomeName();
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
        public final BiomeRestriction climate;
        public final boolean describesType;
        private Pattern pattern = null;

        public Descriptor(String description, float weight, BiomeRestriction climate, boolean describesType)
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
    
    public static class BiomeRestriction {
    	public final float minTemperature, maxTemperature;
        public final float minRainfall, maxRainfall;
        public final int minTreesPerChunk, maxTreesPerChunk;
        public final float minHeightVariation, maxHeightVariation;
        
        public BiomeRestriction(float minTemperature, float maxTemperature, float minRainfall, float maxRainfall,
        		int minTreesPerChunk, int maxTreesPerChunk, float minHeightVariation, float maxHeightVariation) {
        	this.minTemperature = minTemperature;
			this.maxTemperature = maxTemperature;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
			this.minTreesPerChunk = minTreesPerChunk;
			this.maxTreesPerChunk = maxTreesPerChunk;
			this.minHeightVariation = minHeightVariation;
			this.maxHeightVariation = maxHeightVariation;
        }
        
        public BiomeRestriction() {
        	this.minTemperature = this.minRainfall = this.minHeightVariation = Float.NEGATIVE_INFINITY;
			this.maxTemperature = this.maxRainfall = this.maxHeightVariation = Float.POSITIVE_INFINITY;
			this.minTreesPerChunk = Integer.MIN_VALUE;
			this.maxTreesPerChunk = Integer.MAX_VALUE;
        }
        
        public boolean isCompatible(Biome biome) {
			return biome.getTemperature() >= minTemperature && biome.getTemperature() <= maxTemperature &&
				   biome.getRainfall() >= minRainfall && biome.getRainfall() <= maxRainfall &&
				   biome.theBiomeDecorator.treesPerChunk >= minTreesPerChunk &&
				   biome.theBiomeDecorator.treesPerChunk <= maxTreesPerChunk &&
				   biome.getHeightVariation() >= minHeightVariation &&
				   biome.getHeightVariation() <= maxHeightVariation;
		}
    }

}
