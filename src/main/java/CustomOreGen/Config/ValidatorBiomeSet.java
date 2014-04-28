package CustomOreGen.Config;

import java.util.Collection;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Util.BiomeDescriptor;

public class ValidatorBiomeSet extends ValidatorNode {
	
	public BiomeDescriptor biomeSet;
	public float weight = 1.0F;
	
	protected ValidatorBiomeSet(ValidatorNode parent, Node node) {
		super(parent, node);
	}

	protected boolean validateChildren() throws ParserException
	{
		super.validateChildren();
		
		this.biomeSet = new BiomeDescriptor();
		String name = this.validateNamedAttribute(String.class, "name", null, true);
		if (name != null) {
			this.biomeSet.setName(name);
			this.getParser().target.getBiomeSets().add(this.biomeSet);
		}
		
		this.weight = this.validateNamedAttribute(Float.class, "Weight", this.weight, true);
		
		String inherits = this.validateNamedAttribute(String.class, "inherits", null, true);
		if (inherits != null)	
		{
			Collection<BiomeDescriptor> sets = this.getParser().target.getBiomeSets(inherits);

			if (sets.isEmpty())
			{
				throw new ParserException("Cannot inherit biomes (\'" + inherits + "\' is not a loaded biome set).", this.getNode());
			}

			if (sets.size() > 1)
			{
	            throw new ParserException("Cannot inherit biomes (\'" + inherits + "\' is ambiguous; matching " + sets.size() + " loaded biome sets).", this.getNode());
			}

			try
			{
				this.biomeSet.addAll(sets.iterator().next(), this.weight);
			}
			catch (IllegalArgumentException e)
			{
				throw new ParserException("Cannot inherit biomes (" + e.getMessage() + ").", this.getNode(), e);
			}
		}
	        
		validateBiomes();
		
		this.getNode().setUserData("validated", true, null);
		
		return true;
	}
	    
	public void validateBiomes() throws ParserException {
		for (ValidatorBiomeDescriptor biome : validateNamedChildren(2, "Biome", new ValidatorBiomeDescriptor.Factory())) {
			this.biomeSet.add(biome.biome, biome.weight * this.weight, biome.climate, false);
        }
        for (ValidatorBiomeDescriptor biomeType : validateNamedChildren(2, "BiomeType", new ValidatorBiomeDescriptor.Factory())) {
        	this.biomeSet.add(biomeType.biome, biomeType.weight * this.weight, biomeType.climate, true);
        }
        for (ValidatorBiomeSet biomeSet : validateNamedChildren(2, "BiomeSet", new ValidatorBiomeSet.Factory())) {
        	this.biomeSet.addAll(biomeSet.biomeSet, this.weight);
        }
	}
	
	public static class Factory implements IValidatorFactory<ValidatorBiomeSet>
	{
		public ValidatorBiomeSet createValidator(ValidatorNode parent, Node node)
		{
			return new ValidatorBiomeSet(parent, node);
		}
	}

}
