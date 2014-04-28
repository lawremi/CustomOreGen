package CustomOreGen.Config;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Config.ExpressionEvaluator.EvaluatorException;

public class ValidatorExpression extends ValidatorSimpleNode
{
    protected ValidatorExpression(ValidatorNode parent, Node node, ExpressionEvaluator evaluator)
    {
        super(parent, node, String.class, evaluator);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
        this.checkChildrenValid();
        Object value = null;

        try
        {
            value = this.evaluator.evaluate((String)this.content);
        }
        catch (EvaluatorException var3)
        {
            throw new ParserException(var3.getMessage(), this.getNode(), var3);
        }

        this.replaceWithNode(new Node[] {value == null ? null : this.getNode().getOwnerDocument().createTextNode(value.toString())});
        return false;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorExpression>
    {
        private final ExpressionEvaluator _evaluator;

        public Factory(ExpressionEvaluator evaluator)
        {
            this._evaluator = evaluator;
        }

        public ValidatorExpression createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorExpression(parent, node, this._evaluator);
        }
    }

}
