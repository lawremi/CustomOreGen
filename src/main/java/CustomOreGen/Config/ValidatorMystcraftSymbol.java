package CustomOreGen.Config;

import org.w3c.dom.Node;

/* Simply ignores MystcraftSymbol definitions, which have long been obsolete */

public class ValidatorMystcraftSymbol extends ValidatorNode
{
    protected ValidatorMystcraftSymbol(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorMystcraftSymbol>
    {
        public ValidatorMystcraftSymbol createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorMystcraftSymbol(parent, node);
        }
    }

}
