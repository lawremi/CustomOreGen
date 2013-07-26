package CustomOreGen.Config;

import java.util.Collection;
import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

public class ValidatorRoot extends ValidatorNode
{
    private final Collection<String> _topLevelNodes;

    protected ValidatorRoot(ValidatorNode parent, Node node, Collection<String> topLevelNodes)
    {
        super(parent, node);
        this._topLevelNodes = topLevelNodes;
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        Node parent = this.getNode().getParentNode();

        if (parent != null && parent.getNodeType() == 9)
        {
            this.getNode().setUserData("validated", true, (UserDataHandler)null);

            if (this._topLevelNodes != null)
            {
            	for (String nodeName : this._topLevelNodes) {
            		this.validateNamedChildren(2, nodeName, (IValidatorFactory)null);
            	}
            }
        }

        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorRoot>
    {
        private final Collection<String> _topLevelNodes;

        public Factory(Collection<String> topLevelNodes)
        {
            this._topLevelNodes = topLevelNodes;
        }

        public ValidatorRoot createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorRoot(parent, node, this._topLevelNodes);
        }
    }

}
