package CustomOreGen.Config;

import CustomOreGen.MystcraftSymbolData;
import org.w3c.dom.Node;

public class ValidatorMystcraftSymbol extends ValidatorNode
{
    protected ValidatorMystcraftSymbol(ValidatorNode parent, Node node)
    {
        super(parent, node);
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        String symbolName = (String)this.validateRequiredAttribute(String.class, "name", true);
        MystcraftSymbolData symbolData = this.getParser().target.getMystcraftSymbol(symbolName);

        if (symbolData != null)
        {
            throw new ParserException("A symbol named \'" + symbolData.symbolName + "\' already exists.", this.getNode());
        }
        else
        {
            symbolData = new MystcraftSymbolData(this.getParser().target.world, symbolName);
            this.getParser().target.getMystcraftSymbols().add(symbolData);
            symbolData.displayName = (String)this.validateNamedAttribute(String.class, "displayName", symbolData.displayName, true);
            symbolData.weight = ((Float)this.validateNamedAttribute(Float.class, "weight", Float.valueOf(symbolData.weight), true)).floatValue();
            symbolData.instability = ((Float)this.validateNamedAttribute(Float.class, "instability", Float.valueOf(symbolData.instability), true)).floatValue();
            return true;
        }
    }

    private void checkSymbolProperty(String description, Object worldProp, Object dimProp) throws ParserException
    {
        if (worldProp != dimProp && (worldProp == null || !worldProp.equals(dimProp)))
        {
            throw new ParserException("Dimension-specific " + description + " are not allowed (\'" + dimProp + "\' != \'" + worldProp + "\')", this.getNode());
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorMystcraftSymbol>
    {
        public ValidatorMystcraftSymbol createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorMystcraftSymbol(parent, node);
        }
    }

}
