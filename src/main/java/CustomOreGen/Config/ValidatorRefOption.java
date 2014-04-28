package CustomOreGen.Config;

import CustomOreGen.Server.ConfigOption;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

public class ValidatorRefOption extends ValidatorNode
{
    public ValidatorRefOption(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        String optionName = (String)this.validateRequiredAttribute(String.class, "name", true);
        ConfigOption option = this.getParser().target.getConfigOption(optionName);

        if (option == null)
        {
            throw new ParserException("Option \'" + optionName + "\' is not a recognized option.", this.getNode());
        }
        else
        {
            this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
            this.checkChildrenValid();
            Object value = option.getValue();
            this.replaceWithNode(new Node[] {value == null ? null : this.getNode().getOwnerDocument().createTextNode(value.toString())});
            return false;
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorRefOption>
    {
        public ValidatorRefOption createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorRefOption(parent, node);
        }
    }

}
