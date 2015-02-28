package CustomOreGen.Config;

import net.minecraftforge.oredict.OreDictionary;

import org.w3c.dom.Node;

import CustomOreGen.Config.ValidatorNode.IValidatorFactory;
import cpw.mods.fml.common.Loader;

public class ValidatorIfOreExists extends ValidatorCondition {

    protected ValidatorIfOreExists(ValidatorNode parent, Node node, boolean invert)
    {
        super(parent, node, invert);
    }

    protected boolean evaluateCondition() throws ParserException
    {
        String oreName = (String)this.validateRequiredAttribute(String.class, "name", true);
        return OreDictionary.getOres(oreName).size() > 0;
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
