package CustomOreGen.Config;

import net.minecraft.src.ModLoader;
import org.w3c.dom.Node;

import CustomOreGen.Config.ValidatorNode.IValidatorFactory;

public class ValidatorIfModInstalled extends ValidatorCondition
{
    protected ValidatorIfModInstalled(ValidatorNode parent, Node node, boolean invert)
    {
        super(parent, node, invert);
    }

    protected boolean evaluateCondition() throws ParserException
    {
        String modName = (String)this.validateRequiredAttribute(String.class, "name", true);
        return ModLoader.isModLoaded(modName);
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
