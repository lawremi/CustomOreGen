package CustomOreGen.Config;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

public class ValidatorSection extends ValidatorNode
{
    protected ValidatorSection(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
        this.replaceWithNodeContents(new Node[] {this.getNode()});
        return false;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorSection>
    {
        public ValidatorSection createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorSection(parent, node);
        }
    }

}
