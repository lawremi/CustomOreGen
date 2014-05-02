package CustomOreGen.Config;

import org.w3c.dom.Node;

import cpw.mods.fml.common.Loader;

public class ValidatorIfModInstalled extends ValidatorCondition
{
    protected ValidatorIfModInstalled(ValidatorNode parent, Node node, boolean invert)
    {
        super(parent, node, invert);
    }

    protected boolean evaluateCondition() throws ParserException
    {
        String modName = (String)this.validateRequiredAttribute(String.class, "name", true);
        return Loader.isModLoaded(modName);
    }
    
    public static class Factory implements IValidatorFactory<ValidatorIfModInstalled>
    {
        private final boolean _invert;

        public Factory(boolean invert)
        {
            this._invert = invert;
        }

        public ValidatorIfModInstalled createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorIfModInstalled(parent, node, this._invert);
        }
    }

}
