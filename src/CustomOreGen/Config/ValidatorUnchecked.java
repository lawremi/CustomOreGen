package CustomOreGen.Config;

import org.w3c.dom.Node;

public class ValidatorUnchecked extends ValidatorNode
{
    protected ValidatorUnchecked(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        return false;
    }
    

    public static class Factory implements IValidatorFactory<ValidatorUnchecked>
    {
    	public ValidatorUnchecked createValidator(ValidatorNode parent, Node node)
    	{
    		return new ValidatorUnchecked(parent, node);
    	}
    }

}
