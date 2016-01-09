package CustomOreGen.Config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import CustomOreGen.Server.IOreDistribution;
import CustomOreGen.Server.IOreDistribution.IDistributionFactory;
import CustomOreGen.Server.IOreDistribution.StandardSettings;
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
            this.distribution = this._distributionFactory.createDistribution(this.getParser().target.nextDistributionID());
        }
        catch (Exception var7)
        {
            throw new ParserException("Failed to create distribution using \'" + this._distributionFactory + "\'.", this.getNode(), var7);
        }

        this.getNode().setUserData("value", this.distribution, (UserDataHandler)null);
        super.validateChildren();
        String inherits = this.validateNamedAttribute(String.class, "Inherits", null, true);

        if (inherits != null)
        {
            Collection<IOreDistribution> settings = this.getParser().target.getOreDistributions(inherits);

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
                this.distribution.inheritFrom(settings.iterator().next());
            }
            catch (IllegalArgumentException var6)
            {
                throw new ParserException("Cannot inherit settings (" + var6.getMessage() + ").", this.getNode(), var6);
            }
        }

        HashSet<String> settings1 = new HashSet<String>(this.distribution.getDistributionSettingDescriptions().keySet());
        String nameKey = IOreDistribution.StandardSettings.Name.name();

        if (settings1.contains(nameKey))
        {
            String newName = this.validateNamedAttribute(String.class, nameKey, null, true);

            try
            {
                if (newName != null)
                {
                	this.getParser().target.registerDistribution(newName, this.distribution);
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
        
        String displayNameKey = IOreDistribution.StandardSettings.DisplayName.name();

        if (settings1.contains(displayNameKey))
        {
            String newName = this.validateNamedAttribute(String.class, displayNameKey, null, true);

            if (newName == null) {
            	newName = (String)this.distribution.getDistributionSetting(nameKey);
            }
            
            try
            {
                if (newName != null)
                {
                    this.distribution.setDistributionSetting(displayNameKey, newName);
                }
            }
            catch (IllegalAccessException e)
            {
                throw new ParserException("Attribute \'" + displayNameKey + "\' is not configurable.", this.getNode(), e);
            }
            catch (IllegalArgumentException e)
            {
                throw new ParserException("Attribute \'" + displayNameKey + "\' cannot be set (" + e.getMessage() + ").", this.getNode(), e);
            }

            settings1.remove(displayNameKey);
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

        String oreBlockKey = IOreDistribution.StandardSettings.OreBlock.name();
        
        if (settings.contains(oreBlockKey))
        {
            BlockDescriptor oreBlockDesc = new BlockDescriptor();
            String oreBlockName = this.validateNamedAttribute(String.class, "Block", null, true);

            if (oreBlockName != null)
            {
                oreBlockDesc.add(oreBlockName);
            }

            for (ValidatorBlockDescriptor oreBlock : validateNamedChildren(2, "OreBlock", new ValidatorBlockDescriptor.Factory())) {
                oreBlockDesc.add(oreBlock.blocks, oreBlock.weight, oreBlock.nbt);	
            }
            
            for (ValidatorBlockDescriptor oreBlock : validateNamedChildren(2, "FirstOreDictBlock", new ValidatorBlockDescriptor.Factory())) {
                oreBlockDesc.add(oreBlock.blocks, oreBlock.weight, true, true, false, oreBlock.nbt);	
            }
            
            if (!oreBlockDesc.getDescriptors().isEmpty())
            {
                try
                {
                    this.distribution.setDistributionSetting(oreBlockKey, oreBlockDesc);
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

            settings.remove(oreBlockKey);
        }

        validatePlacementRestriction(settings, IOreDistribution.StandardSettings.Replaces);
        validatePlacementRestriction(settings, IOreDistribution.StandardSettings.PlacesAbove);
        validatePlacementRestriction(settings, IOreDistribution.StandardSettings.PlacesBelow);
        validatePlacementRestriction(settings, IOreDistribution.StandardSettings.PlacesBeside);

        String biomeKey = IOreDistribution.StandardSettings.TargetBiome.name();

        if (settings.contains(biomeKey))
        {
            BiomeDescriptor biomeDescriptor = new BiomeDescriptor();
            
            for (ValidatorBiomeDescriptor biome : validateNamedChildren(2, "Biome", new ValidatorBiomeDescriptor.Factory())) {
            	biomeDescriptor.add(biome.biome, biome.weight, biome.restriction, false);
            }

            for (ValidatorBiomeDescriptor biomeType : validateNamedChildren(2, "BiomeType", new ValidatorBiomeDescriptor.Factory())) {
            	biomeDescriptor.add(biomeType.biome, biomeType.weight, biomeType.restriction, true);
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
        
        for (String settingName : settings) {
        	Object setting = this.distribution.getDistributionSetting(settingName);

            if (setting != null)
            {
                if (setting instanceof PDist)
                {
                    continue;
                }

                setting = this.validateNamedAttribute(setting.getClass(), settingName, setting, true);
            }
            else
            {
                setting = this.validateNamedAttribute(String.class, settingName, null, true);
            }

            try
            {
                if (setting != null)
                {
                    this.distribution.setDistributionSetting(settingName, setting);
                }
            }
            catch (IllegalAccessException var10)
            {
                throw new ParserException("Attribute \'" + settingName + "\' is not configurable.", this.getNode(), var10);
            }
            catch (IllegalArgumentException var11)
            {
                throw new ParserException("Attribute \'" + settingName + "\' cannot be set (" + var11.getMessage() + ").", this.getNode(), var11);
            }
        }
    }

	private void validatePlacementRestriction(Set<String> settings, StandardSettings setting) throws ParserException {
		String settingKey = setting.name();
		
		if (settings.contains(settingKey))
        {
            BlockDescriptor replacesDesc = new BlockDescriptor();
            for (ValidatorBlockDescriptor replaces : validateNamedChildren(2, settingKey, new ValidatorBlockDescriptor.Factory())) {
            	replacesDesc.add(replaces.blocks, replaces.weight, false, false, false, null);
            }
            for (ValidatorBlockDescriptor replaces : validateNamedChildren(2, settingKey + "Ore", new ValidatorBlockDescriptor.Factory())) {
            	replacesDesc.add(replaces.blocks, replaces.weight, true, false, false, null);
            }
            for (ValidatorBlockDescriptor replaces : validateNamedChildren(2, settingKey + "Regexp", new ValidatorBlockDescriptor.Factory())) {
            	replacesDesc.add(replaces.blocks, replaces.weight, false, false, true, null);
            }
            
            if (!replacesDesc.getDescriptors().isEmpty())
            {
                try
                {
                    this.distribution.setDistributionSetting(settingKey, replacesDesc);
                }
                catch (IllegalAccessException var14)
                {
                    throw new ParserException("'" + settingKey + "' not configurable.", this.getNode(), var14);
                }
                catch (IllegalArgumentException var15)
                {
                    throw new ParserException("'" + settingKey + "' not supported by this distribution.", this.getNode(), var15);
                }
            }

            settings.remove(settingKey);
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
