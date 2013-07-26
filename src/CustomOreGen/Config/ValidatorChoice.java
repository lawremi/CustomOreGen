package CustomOreGen.Config;

import org.w3c.dom.Node;

public class ValidatorChoice extends ValidatorNode
{
    public String value = null;
    public String displayValue = null;
    public String description = null;

    protected ValidatorChoice(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.value = this.validateRequiredAttribute(String.class, "Value", true);
        this.displayValue = this.validateNamedAttribute(String.class, "DisplayValue", this.displayValue, true);
        this.description = this.validateNamedAttribute(String.class, "Description", null, true);
        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorChoice>
    {
        public ValidatorChoice createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorChoice(parent, node);
        }
    }

}
