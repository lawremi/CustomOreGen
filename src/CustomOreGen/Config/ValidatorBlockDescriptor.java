package CustomOreGen.Config;

import org.w3c.dom.Node;

public class ValidatorBlockDescriptor extends ValidatorNode
{
    public String blocks = null;
    public float weight = 1.0F;

    protected ValidatorBlockDescriptor(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.blocks = this.validateRequiredAttribute(String.class, "Block", true);
        this.weight = this.validateNamedAttribute(Float.class, "Weight", this.weight, true);
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
