package CustomOreGen.Config;

import CustomOreGen.Server.ChoiceOption;
import CustomOreGen.Server.ConfigOption;
import org.w3c.dom.Node;

public class ValidatorIfChoice extends ValidatorCondition
{
    protected ValidatorIfChoice(ValidatorNode parent, Node node, boolean invert)
    {
        super(parent, node, invert);
    }

    protected boolean evaluateCondition() throws ParserException
    {
        String optionName = this.validateRequiredAttribute(String.class, "name", true);
        String strValue = this.validateNamedAttribute(String.class, "value", null, true);
        ConfigOption option = this.getParser().target.getConfigOption(optionName);
        boolean isOptionValid = option != null && option instanceof ChoiceOption;

        if (strValue == null)
        {
            return isOptionValid;
        }
        else if (isOptionValid)
        {
            return strValue.equalsIgnoreCase((String)option.getValue());
        }
        else
        {
            throw new ParserException("Option \'" + optionName + "\' is not a recognized Choice option.", this.getNode());
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorIfChoice>
    {
        private final boolean _invert;

        public Factory(boolean invert)
        {
            this._invert = invert;
        }

        public ValidatorIfChoice createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorIfChoice(parent, node, this._invert);
        }
    }

}
