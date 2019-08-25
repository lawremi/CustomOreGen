package CustomOreGen.Config;

import org.w3c.dom.Node;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;

public class ValidatorBlockDescriptor extends ValidatorNode
{
    public String blocks = null;
    public float weight = 1.0F;
	public CompoundNBT nbt;
	
    protected ValidatorBlockDescriptor(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.blocks = this.validateRequiredAttribute(String.class, "Block", true);
        this.weight = this.validateNamedAttribute(Float.class, "Weight", this.weight, true);
        String nbtJson = this.validateNamedAttribute(String.class, "NBT", null, true);
        if (nbtJson != null) {
        	try {
        		this.nbt = JsonToNBT.getTagFromJson(nbtJson);
			} catch (Exception e) {
				throw new ParserException("Failed to parse JSON", e);
			}
        }
        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorBlockDescriptor>
    {
        public ValidatorBlockDescriptor createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorBlockDescriptor(parent, node);
        }
    }

}
