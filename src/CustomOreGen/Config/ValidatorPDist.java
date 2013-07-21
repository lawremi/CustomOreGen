package CustomOreGen.Config;

import org.w3c.dom.Node;

import CustomOreGen.Server.IOreDistribution;
import CustomOreGen.Util.PDist;
import CustomOreGen.Util.PDist.Type;

public class ValidatorPDist extends ValidatorNode
{
    private final IOreDistribution _parentDist;
    public String name = null;
    public PDist pdist = null;

    protected ValidatorPDist(ValidatorNode parent, Node node, IOreDistribution parentDistribution)
    {
        super(parent, node);
        this._parentDist = parentDistribution;
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        this.name = (String)this.validateRequiredAttribute(String.class, "Name", true);

        if (this._parentDist == null)
        {
            this.pdist = new PDist();
        }
        else
        {
            try
            {
                this.pdist = (PDist)this._parentDist.getDistributionSetting(this.name);
            }
            catch (ClassCastException var4)
            {
                throw new ParserException("Setting \'" + this.name + "\' is not supported by this distribution.", this.getNode(), var4);
            }

            if (this.pdist == null)
            {
                throw new ParserException("Setting \'" + this.name + "\' is not supported by this distribution.", this.getNode());
            }
        }

        this.pdist.mean = ((Float)this.validateNamedAttribute(Float.class, "Avg", Float.valueOf(this.pdist.mean), true)).floatValue();
        this.pdist.range = ((Float)this.validateNamedAttribute(Float.class, "Range", Float.valueOf(this.pdist.range), true)).floatValue();
        this.pdist.type = (Type)this.validateNamedAttribute(Type.class, "Type", this.pdist.type, true);

        if (this._parentDist != null)
        {
            try
            {
                this._parentDist.setDistributionSetting(this.name, this.pdist);
            }
            catch (IllegalAccessException var2)
            {
                throw new ParserException("Setting \'" + this.name + "\' is not configurable.", this.getNode(), var2);
            }
            catch (IllegalArgumentException var3)
            {
                throw new ParserException("Setting \'" + this.name + "\' is not supported by this distribution.", this.getNode(), var3);
            }
        }

        return true;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorPDist>
    {
        private final IOreDistribution _parentDist;

        public Factory(IOreDistribution parentDistribution)
        {
            this._parentDist = parentDistribution;
        }

        public ValidatorPDist createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorPDist(parent, node, this._parentDist);
        }
    }

}
