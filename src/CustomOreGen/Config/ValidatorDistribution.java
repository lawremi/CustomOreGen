package CustomOreGen.Config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Server.IOreDistribution;
import CustomOreGen.Server.IOreDistribution.IDistributionFactory;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.PDist;

public class ValidatorDistribution extends ValidatorNode
{
    private final IDistributionFactory _distributionFactory;
    public IOreDistribution distribution = null;

    protected ValidatorDistribution(ValidatorNode parent, Node node, IDistributionFactory distributionFactory)
    {
        super(parent, node);
        this._distributionFactory = distributionFactory;
    }

    protected boolean validateChildren() throws ParserException
    {
        try
        {
            this.distribution = this._distributionFactory.createDistribution(this.getParser().target.getOreDistributions().size());
        }
        catch (Exception var7)
        {
            throw new ParserException("Failed to create distribution using \'" + this._distributionFactory + "\'.", this.getNode(), var7);
        }

        this.getParser().target.getOreDistributions().add(this.distribution);
        this.getNode().setUserData("value", this.distribution, (UserDataHandler)null);
        super.validateChildren();
        String inherits = this.validateNamedAttribute(String.class, "Inherits", null, true);

        if (inherits != null)
        {
            Collection settings = this.getParser().target.getOreDistributions(inherits);

            if (settings.isEmpty())
            {
                throw new ParserException("Cannot inherit settings (\'" + inherits + "\' is not a loaded distribution).", this.getNode());
            }

            if (settings.size() > 1)
            {
                throw new ParserException("Cannot inherit settings (\'" + inherits + "\' is ambiguous; matching " + settings.size() + " loaded distributions).", this.getNode());
            }

            try
            {
                this.distribution.inheritFrom((IOreDistribution)settings.iterator().next());
            }
            catch (IllegalArgumentException var6)
            {
                throw new ParserException("Cannot inherit settings (" + var6.getMessage() + ").", this.getNode(), var6);
            }
        }

        HashSet settings1 = new HashSet(this.distribution.getDistributionSettings().keySet());
        String nameKey = IOreDistribution.StandardSettings.Name.name();

        if (settings1.contains(nameKey))
        {
            String newName = this.validateNamedAttribute(String.class, nameKey, null, true);

            try
            {
                if (newName != null)
                {
                    this.distribution.setDistributionSetting(nameKey, newName);
                }
            }
            catch (IllegalAccessException var8)
            {
                throw new ParserException("Attribute \'" + nameKey + "\' is not configurable.", this.getNode(), var8);
            }
            catch (IllegalArgumentException var9)
            {
                throw new ParserException("Attribute \'" + nameKey + "\' cannot be set (" + var9.getMessage() + ").", this.getNode(), var9);
            }

            settings1.remove(nameKey);
        }

        this.validateDistributionSettings(settings1);
        return true;
    }

    public void validateDistributionSettings(Set<String> settings) throws ParserException
    {
        String parentKey = IOreDistribution.StandardSettings.Parent.name();

        if (settings.contains(parentKey))
        {
            for (Node oreBlockKey = this.getNode().getParentNode(); oreBlockKey != null; oreBlockKey = oreBlockKey.getParentNode())
            {
                Object replacesKey = oreBlockKey.getUserData("value");

                if (replacesKey != null && replacesKey instanceof IOreDistribution)
                {
                    try
                    {
                        this.distribution.setDistributionSetting(parentKey, replacesKey);
                    }
                    catch (IllegalAccessException var18)
                    {
                        throw new ParserException("Parent distribution is not configurable.", this.getNode(), var18);
                    }
                    catch (IllegalArgumentException var19)
                    {
                        throw new ParserException("Invalid parent distribution.", this.getNode(), var19);
                    }

                    this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
                    break;
                }
            }

            settings.remove(parentKey);
        }

        String oreBlockKey1 = IOreDistribution.StandardSettings.OreBlock.name();
        String biomeKey;

        if (settings.contains(oreBlockKey1))
        {
            BlockDescriptor replacesKey1 = new BlockDescriptor();
            biomeKey = this.validateNamedAttribute(String.class, "Block", null, true);

            if (biomeKey != null)
            {
                replacesKey1.add(biomeKey);
            }

            for (ValidatorBlockDescriptor settingName : validateNamedChildren(2, "OreBlock", new ValidatorBlockDescriptor.Factory())) {
                replacesKey1.add(settingName.blocks, settingName.weight);	
            }
            
            if (!replacesKey1.getDescriptors().isEmpty())
            {
                try
                {
                    this.distribution.setDistributionSetting(oreBlockKey1, replacesKey1);
                }
                catch (IllegalAccessException var16)
                {
                    throw new ParserException("Target ore blocks are not configurable.", this.getNode(), var16);
                }
                catch (IllegalArgumentException var17)
                {
                    throw new ParserException("Target ore blocks are not supported by this distribution.", this.getNode(), var17);
                }
            }

            settings.remove(oreBlockKey1);
        }

        String replacesKey2 = IOreDistribution.StandardSettings.ReplaceableBlock.name();

        if (settings.contains(replacesKey2))
        {
            BlockDescriptor biomeKey1 = new BlockDescriptor();
            for (ValidatorBlockDescriptor settingName : validateNamedChildren(2, "Replaces", new ValidatorBlockDescriptor.Factory())) {
            	biomeKey1.add(settingName.blocks, settingName.weight);
            }
            
            if (!biomeKey1.getDescriptors().isEmpty())
            {
                try
                {
                    this.distribution.setDistributionSetting(replacesKey2, biomeKey1);
                }
                catch (IllegalAccessException var14)
                {
                    throw new ParserException("Replaceable blocks are not configurable.", this.getNode(), var14);
                }
                catch (IllegalArgumentException var15)
                {
                    throw new ParserException("Replaceable blocks are not supported by this distribution.", this.getNode(), var15);
                }
            }

            settings.remove(replacesKey2);
        }

        biomeKey = IOreDistribution.StandardSettings.TargetBiome.name();

        if (settings.contains(biomeKey))
        {
            BiomeDescriptor biomeDescriptor = new BiomeDescriptor();
            
            for (ValidatorBiomeDescriptor biome : validateNamedChildren(2, "Biome", new ValidatorBiomeDescriptor.Factory())) {
            	biomeDescriptor.add(biome.biome, biome.weight, biome.climate, false);
            }

            for (ValidatorBiomeDescriptor biomeType : validateNamedChildren(2, "BiomeType", new ValidatorBiomeDescriptor.Factory())) {
            	biomeDescriptor.add(biomeType.biome, biomeType.weight, biomeType.climate, true);
            }
            
            for (ValidatorBiomeSet biomeSet : validateNamedChildren(2, "BiomeSet", new ValidatorBiomeSet.Factory())) {
            	biomeDescriptor.addAll(biomeSet.biomeSet, 1.0F);
            }
            
            if (!biomeDescriptor.getDescriptors().isEmpty())
            {
                try
                {
                    this.distribution.setDistributionSetting(biomeKey, biomeDescriptor);
                }
                catch (IllegalAccessException e)
                {
                    throw new ParserException("Biomes are not configurable.", this.getNode(), e);
                }
                catch (IllegalArgumentException e)
                {
                    throw new ParserException("Biomes are not supported by this distribution.", this.getNode(), e);
                }
            }

            settings.remove(biomeKey);
        }

        validateNamedChildren(2, "Setting", new ValidatorPDist.Factory(this.distribution));
        
        for (String settingName3 : settings) {
        	Object value1 = this.distribution.getDistributionSetting(settingName3);

            if (value1 != null)
            {
                if (value1 instanceof PDist)
                {
                    continue;
                }

                value1 = this.validateNamedAttribute((Class<Object>)value1.getClass(), settingName3, value1, true);
            }
            else
            {
                value1 = this.validateNamedAttribute(String.class, settingName3, null, true);
            }

            try
            {
                if (value1 != null)
                {
                    this.distribution.setDistributionSetting(settingName3, value1);
                }
            }
            catch (IllegalAccessException var10)
            {
                throw new ParserException("Attribute \'" + settingName3 + "\' is not configurable.", this.getNode(), var10);
            }
            catch (IllegalArgumentException var11)
            {
                throw new ParserException("Attribute \'" + settingName3 + "\' cannot be set (" + var11.getMessage() + ").", this.getNode(), var11);
            }
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorDistribution>
    {
        private final IDistributionFactory _distributionFactory;

        public Factory(IDistributionFactory distributionFactory)
        {
            this._distributionFactory = distributionFactory;
        }

        public ValidatorDistribution createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorDistribution(parent, node, this._distributionFactory);
        }
    }

}
