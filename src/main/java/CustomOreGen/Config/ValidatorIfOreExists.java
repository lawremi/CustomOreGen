package CustomOreGen.Config;

import org.w3c.dom.Node;

public class ValidatorIfOreExists extends ValidatorCondition {

    protected ValidatorIfOreExists(ValidatorNode parent, Node node, boolean invert)
    {
        super(parent, node, invert);
    }

    protected boolean evaluateCondition() throws ParserException
    {
        String oreName = (String)this.validateRequiredAttribute(String.class, "name", true);
        return ConfigParser.oreExists(oreName);
    }
    
    public static class Factory implements IValidatorFactory<ValidatorIfOreExists>
    {
        private final boolean _invert;

        public Factory(boolean invert)
        {
            this._invert = invert;
        }

        public ValidatorIfOreExists createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorIfOreExists(parent, node, this._invert);
        }
    }

}
