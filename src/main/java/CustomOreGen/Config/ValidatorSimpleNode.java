package CustomOreGen.Config;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Config.ExpressionEvaluator.EvaluatorException;

public class ValidatorSimpleNode extends ValidatorNode
{
    public Object content = null;
    public ExpressionEvaluator evaluator;
    private final Class _targetClass;

    protected ValidatorSimpleNode(ValidatorNode parent, Node node, Class targetClass, ExpressionEvaluator evaluator)
    {
        super(parent, node);
        this._targetClass = targetClass;
        this.evaluator = (ExpressionEvaluator)(evaluator == null ? this.getParser().defaultEvaluator : evaluator);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        StringBuilder buffer = new StringBuilder();

        for (Node input = this.getNode().getFirstChild(); input != null; input = input.getNextSibling())
        {
            if (input.getNodeType() == 3)
            {
                input.setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
                buffer.append(input.getNodeValue());
            }
        }

        String input1 = buffer.toString().trim();

        try
        {
            if (input1.startsWith(":="))
            {
                Object ex = this.evaluator.evaluate(input1.substring(2));

                if (ex == null)
                {
                    this.content = null;
                }
                else if (this._targetClass.isInstance(ex))
                {
                    this.content = ex;
                }
                else if (ex instanceof Number && Number.class.isAssignableFrom(this._targetClass))
                {
                    this.getParser();
                    this.content = ConfigParser.convertNumber(this._targetClass, (Number)ex);
                }
                else
                {
                    this.content = ConfigParser.parseString(this._targetClass, ex.toString());
                }
            }
            else
            {
                this.content = ConfigParser.parseString(this._targetClass, input1);
            }

            return true;
        }
        catch (IllegalArgumentException var4)
        {
            throw new ParserException(var4.getMessage(), this.getNode(), var4);
        }
        catch (EvaluatorException var5)
        {
            throw new ParserException(var5.getMessage(), this.getNode(), var5);
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorSimpleNode>
    {
        private final Class _targetClass;
        private final ExpressionEvaluator _evaluator;

        public Factory(Class targetClass, ExpressionEvaluator evaluator)
        {
            this._targetClass = targetClass;
            this._evaluator = evaluator;
        }

        public Factory(Class targetClass)
        {
            this._targetClass = targetClass;
            this._evaluator = null;
        }

        public ValidatorSimpleNode createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorSimpleNode(parent, node, this._targetClass, this._evaluator);
        }
    }

}
