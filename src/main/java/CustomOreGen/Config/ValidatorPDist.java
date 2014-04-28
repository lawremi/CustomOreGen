package CustomOreGen.Config;

import org.w3c.dom.Node;

import CustomOreGen.Server.IOreDistribution;
import CustomOreGen.Util.HeightScale;
import CustomOreGen.Util.HeightScaledPDist;
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

        HeightScaledPDist heightScaledPDist = null;
        
        if (this._parentDist == null)
        {
            this.pdist = new PDist();
        }
        else
        {
        	Object setting = this._parentDist.getDistributionSetting(this.name);
        	if (setting instanceof PDist) {
        		this.pdist = (PDist)setting;
        	} else if (setting instanceof HeightScaledPDist) {
        		heightScaledPDist = (HeightScaledPDist)setting;
        		this.pdist = heightScaledPDist.pdist;
        	} else {
        		throw new ParserException("Setting \'" + this.name + "\' is not supported by this distribution.", this.getNode());
        	}
            
            if (this.pdist == null)
            {
                throw new ParserException("Setting \'" + this.name + "\' is not supported by this distribution.", this.getNode());
            }
        }

        this.pdist.mean =  this.validateNamedAttribute(Float.class, "Avg", this.pdist.mean, true);
        this.pdist.range = this.validateNamedAttribute(Float.class, "Range", this.pdist.range, true);
        this.pdist.type = this.validateNamedAttribute(Type.class, "Type", this.pdist.type, true);
        
        HeightScaleType scaleType = this.validateNamedAttribute(HeightScaleType.class, "ScaleTo", null, true);
        if (scaleType != null) {
        	if (heightScaledPDist == null) {
        		throw new ParserException("Setting '" + this.name + "' does not support height scaling.", this.getNode());
        	} else {
        		heightScaledPDist.scaleTo = scaleType.getHeightScale();
        	}
        }
        
        if (this._parentDist != null)
        {
            try
            {
                this._parentDist.setDistributionSetting(this.name, heightScaledPDist != null ? heightScaledPDist: this.pdist);
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
