package CustomOreGen.Config;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Config.ValidatorNode.IValidatorFactory;

public class ValidatorAnnotation extends ValidatorSimpleNode
{
    protected ValidatorAnnotation(ValidatorNode parent, Node node)
    {
        super(parent, node, String.class, (ExpressionEvaluator)null);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
        return false;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorAnnotation>
    {
        public ValidatorAnnotation createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorAnnotation(parent, node);
        }
    }

}
