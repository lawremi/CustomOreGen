package CustomOreGen.Config;

import java.util.Iterator;

import org.w3c.dom.Node;

import CustomOreGen.CustomOreGenBase;
import CustomOreGen.Server.ChoiceOption;
import CustomOreGen.Server.ConfigOption;
import CustomOreGen.Server.NumericOption;

public class ValidatorOption extends ValidatorNode
{
    private final Class _type;

    protected ValidatorOption(ValidatorNode parent, Node node, Class type)
    {
        super(parent, node);
        this._type = type;
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        String optionName = this.validateRequiredAttribute(String.class, "name", true);
        ConfigOption option = null;
        Class valueType = null;

        if (this._type == ChoiceOption.class)
        {
            valueType = String.class;
            ChoiceOption groupName = new ChoiceOption(optionName);
            option = groupName;
            Iterator<ValidatorChoice> defValue = this.validateNamedChildren(2, "Choice", new ValidatorChoice.Factory()).iterator();

            while (defValue.hasNext())
            {
                ValidatorChoice loadedValueStr = defValue.next();
                groupName.addPossibleValue(loadedValueStr.value, loadedValueStr.displayValue, loadedValueStr.description);
            }

            if (groupName.getValue() == null)
            {
                throw new ParserException("Choice option has no possible values.", this.getNode());
            }
        }
        else if (this._type == NumericOption.class)
        {
            valueType = Double.class;
            NumericOption groupName1 = new NumericOption(optionName);
            option = groupName1;
            double defValue1 = this.validateNamedAttribute(valueType, "min", groupName1.getMin(), true);
            double err = this.validateNamedAttribute(valueType, "max", groupName1.getMax(), true);

            if (!groupName1.setLimits(defValue1, err))
            {
                throw new ParserException("Numeric option value range [" + defValue1 + "," + err + "] is invalid.", this.getNode());
            }

            double dmin = this.validateNamedAttribute(valueType, "displayMin", groupName1.getMin(), true);
            double dmax = this.validateNamedAttribute(valueType, "displayMax", groupName1.getMax(), true);
            double dincr = this.validateNamedAttribute(valueType, "displayIncrement", (dmax - dmin) / 100.0D, true);

            if (!groupName1.setDisplayLimits(dmin, dmax, dincr))
            {
                throw new ParserException("Numeric option display range/increment [" + dmin + "," + dmax + "]/" + dincr + " is invalid.", this.getNode());
            }
        }
        else if (this._type == ConfigOption.DisplayGroup.class)
        {
            option = new ConfigOption.DisplayGroup(optionName);
        }

        option.setDisplayState(this.validateNamedAttribute(ConfigOption.DisplayState.class, "displayState", option.getDisplayState(), true));
        option.setDisplayName(this.validateNamedAttribute(String.class, "displayName", option.getDisplayName(), true));
        option.setDescription(this.validateNamedAttribute(String.class, "description", option.getDescription(), true));
        String groupName2 = this.validateNamedAttribute(String.class, "displayGroup", null, true);

        if (groupName2 != null)
        {
            ConfigOption defValue3 = this.getParser().target.getConfigOption(groupName2);

            if (defValue3 == null || !(defValue3 instanceof ConfigOption.DisplayGroup))
            {
                throw new ParserException("Option \'" + groupName2 + "\' is not a recognized Display Group.", this.getNode());
            }

            option.setDisplayGroup((ConfigOption.DisplayGroup)defValue3);
        }

        Object defValue2 = valueType == null ? null : this.validateNamedAttribute(valueType, "default", (Object)null, true);

        if (this.getParser().target.getConfigOption(option.getName()) != null)
        {
            throw new ParserException("An Option named \'" + option.getName() + "\' already exists.", this.getNode());
        }
        else
        {
            this.getParser().target.getConfigOptions().add(option);
            String loadedValueStr1 = this.getParser().target.loadConfigOption(option.getName());

            if (loadedValueStr1 != null)
            {
                String err1 = null;

                try
                {
                    Object ex = ConfigParser.parseString(valueType, loadedValueStr1);

                    if (!((ConfigOption)option).setValue(ex))
                    {
                        err1 = "";
                    }
                }
                catch (IllegalArgumentException var15)
                {
                    err1 = " (" + var15.getMessage() + ")";
                }

                if (err1 == null)
                {
                    return true;
                }

                CustomOreGenBase.log.warning("The saved value \'" + loadedValueStr1 + "\' for Config Option \'" + ((ConfigOption)option).getName() + "\' is invalid" + err1 + ".  " + "The default value \'" + (defValue2 == null ? option.getValue() : defValue2) + "\' will be used instead.");
            }

            if (defValue2 != null && !(option.setValue(defValue2)))
            {
                throw new ParserException("Invalid default value \'" + defValue2 + "\' for option \'" + option.getName() + "\'.", this.getNode());
            }
            else
            {
                return true;
            }
        }
    }
    
    public static class Factory implements IValidatorFactory<ValidatorOption>
    {
        private final Class _type;

        public Factory(Class type)
        {
            this._type = type;
        }

        public ValidatorOption createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorOption(parent, node, this._type);
        }
    }

}
