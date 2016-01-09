package CustomOreGen.Config;

import org.w3c.dom.Node;

import CustomOreGen.Util.BiomeDescriptor.BiomeRestriction;

public class ValidatorBiomeDescriptor extends ValidatorNode
{
    public String biome = null;
    public float weight = 1.0F;
    public BiomeRestriction restriction = new BiomeRestriction(); 

    protected ValidatorBiomeDescriptor(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.biome = this.validateRequiredAttribute(String.class, "Name", true);
        this.weight = this.validateNamedAttribute(Float.class, "Weight", this.weight, true);
        float minTemperature = this.validateNamedAttribute(Float.class, "MinTemperature", restriction.minTemperature, true);
        float maxTemperature = this.validateNamedAttribute(Float.class, "MaxTemperature", restriction.maxTemperature, true);
        float minRainfall = this.validateNamedAttribute(Float.class, "MinRainfall", restriction.minRainfall, true);
        float maxRainfall = this.validateNamedAttribute(Float.class, "MaxRainfall", restriction.maxRainfall, true);
        int minTreesPerChunk = this.validateNamedAttribute(Integer.class, "MinTreesPerChunk", restriction.minTreesPerChunk, true);
        int maxTreesPerChunk = this.validateNamedAttribute(Integer.class, "MaxTreesPerChunk", restriction.maxTreesPerChunk, true);
        float minHeightVariation = this.validateNamedAttribute(Float.class, "MinHeightVariation", restriction.minHeightVariation, true);
        float maxHeightVariation = this.validateNamedAttribute(Float.class, "MaxHeightVariation", restriction.maxHeightVariation, true);
        this.restriction = new BiomeRestriction(
        		minTemperature, maxTemperature, 
        		minRainfall, maxRainfall,
        		minTreesPerChunk, maxTreesPerChunk,
        		minHeightVariation, maxHeightVariation);
        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorBiomeDescriptor>
    {
        public ValidatorBiomeDescriptor createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorBiomeDescriptor(parent, node);
        }
    }

}
