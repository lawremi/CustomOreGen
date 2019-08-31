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
        float minDepth = this.validateNamedAttribute(Float.class, "MinDepth", restriction.minDepth, true);
        float maxDepth = this.validateNamedAttribute(Float.class, "MaxDepth", restriction.maxDepth, true);
        float minScale = this.validateNamedAttribute(Float.class, "MinScale", restriction.minScale, true);
        float maxScale = this.validateNamedAttribute(Float.class, "MaxScale", restriction.maxScale, true);
        this.restriction = new BiomeRestriction(
        		minTemperature, maxTemperature, 
        		minRainfall, maxRainfall,
        		minDepth, maxDepth,
        		minScale, maxScale);
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
