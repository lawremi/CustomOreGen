package CustomOreGen.Config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.minecraft.src.ModLoader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;

import CustomOreGen.Config.ValidatorNode.IValidatorFactory;
import CustomOreGen.Server.ChoiceOption;
import CustomOreGen.Server.ConfigOption;
import CustomOreGen.Server.IOreDistribution;
import CustomOreGen.Server.IOreDistribution.IDistributionFactory;
import CustomOreGen.Server.MapGenCloud;
import CustomOreGen.Server.MapGenClusters;
import CustomOreGen.Server.MapGenVeins;
import CustomOreGen.Server.NumericOption;
import CustomOreGen.Server.WorldConfig;
import CustomOreGen.Server.WorldGenSubstitution;
import CustomOreGen.Util.BiomeDescriptor;
import CustomOreGen.Util.BlockDescriptor;
import CustomOreGen.Util.CIStringMap;

public class ConfigParser
{
    public final WorldConfig target;
    public final ConfigExpressionEvaluator defaultEvaluator = new ConfigExpressionEvaluator(this);
    protected Random rng = null;
    protected final DocumentBuilder domBuilder;
    protected final SAXParser saxParser;
    private static final Map<String,IValidatorFactory> distributionValidators = new HashMap();

    public boolean blockExists(String blockDescription)
    {
        return (new BlockDescriptor(blockDescription)).getTotalMatchWeight() > 0.0F;
    }

    public boolean biomeExists(String biomeDescription)
    {
        return (new BiomeDescriptor(biomeDescription)).getTotalMatchWeight() > 0.0F;
    }

    public float nextRandom()
    {
        return this.rng == null ? 0.0F : this.rng.nextFloat();
    }

    public ConfigParser(WorldConfig target) throws ParserConfigurationException, SAXException
    {
        this.target = target;
        DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
        domBuilderFactory.setNamespaceAware(true);
        domBuilderFactory.setIgnoringComments(true);
        domBuilderFactory.setIgnoringElementContentWhitespace(true);
        domBuilderFactory.setExpandEntityReferences(true);
        this.domBuilder = domBuilderFactory.newDocumentBuilder();
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        this.saxParser = saxParserFactory.newSAXParser();
    }

    public void parseFile(File file) throws ParserConfigurationException, IOException, SAXException
    {
        Document fileDOM = this.domBuilder.newDocument();
        fileDOM.setUserData("value", file, (UserDataHandler)null);
        this.saxParser.parse(file, new LineAwareSAXHandler(fileDOM));
        ValidatorNode validator = new ValidatorNode(this, fileDOM);
        Vector topLevelNodes = new Vector();
        validator.addGlobalValidator(Node.ELEMENT_NODE, "Import", new ValidatorImport.Factory(true));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "OptionalImport", new ValidatorImport.Factory(false));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "Description", new ValidatorAnnotation.Factory());
        validator.addGlobalValidator(Node.ATTRIBUTE_NODE, "Description", new ValidatorAnnotation.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "Comment", new ValidatorAnnotation.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "ConfigSection", new ValidatorSection.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "IfCondition", new ValidatorIfCondition.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "IfChoice", new ValidatorIfChoice.Factory(false));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "IfNotChoice", new ValidatorIfChoice.Factory(true));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "IfModInstalled", new ValidatorIfModInstalled.Factory(false));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "IfNotModInstalled", new ValidatorIfModInstalled.Factory(true));
        validator.addGlobalValidator(Node.ELEMENT_NODE, "GetOption", new ValidatorRefOption.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "Expression", new ValidatorExpression.Factory(this.defaultEvaluator));
        topLevelNodes.add("OptionDisplayGroup");
        validator.addGlobalValidator(Node.ELEMENT_NODE, "OptionDisplayGroup", new ValidatorOption.Factory(ConfigOption.DisplayGroup.class));
        topLevelNodes.add("OptionChoice");
        validator.addGlobalValidator(Node.ELEMENT_NODE, "OptionChoice", new ValidatorOption.Factory(ChoiceOption.class));
        topLevelNodes.add("OptionNumeric");
        validator.addGlobalValidator(Node.ELEMENT_NODE, "OptionNumeric", new ValidatorOption.Factory(NumericOption.class));
        topLevelNodes.add("MystcraftSymbol");
        validator.addGlobalValidator(Node.ELEMENT_NODE, "MystcraftSymbol", new ValidatorMystcraftSymbol.Factory());
        validator.addGlobalValidator(Node.ELEMENT_NODE, "BiomeSet", new ValidatorBiomeSet.Factory());
        
        for (Entry<String,IValidatorFactory> entry : distributionValidators.entrySet()) {
        	validator.addGlobalValidator(Node.ELEMENT_NODE, entry.getKey(), entry.getValue());
            topLevelNodes.add(entry.getKey());
        }
        
        validator.addGlobalValidator(Node.ELEMENT_NODE, "Config", new ValidatorRoot.Factory(topLevelNodes));

        if (this.target.worldInfo == null)
        {
            this.rng = null;
        }
        else
        {
            this.rng = new Random(this.target.worldInfo.getSeed());
            this.rng.nextInt();
        }

        validator.validate();
    }

    public static Object parseString(Class type, String value) throws IllegalArgumentException
    {
        if (type != null && value != null)
        {
            if (type.isAssignableFrom(String.class))
            {
                return value;
            }
            else if (type.isEnum())
            {
            	for (Enum val : (Enum[])type.getEnumConstants()) {
            		if (val.name().equalsIgnoreCase(value))
                    {
                        return val;
                    }
            	}
                throw new IllegalArgumentException("Invalid enumeration value \'" + value + "\'");
            }
            else if (!type.isAssignableFrom(Character.TYPE) && !type.isAssignableFrom(Character.class))
            {
                if (!type.isAssignableFrom(Boolean.TYPE) && !type.isAssignableFrom(Boolean.class))
                {
                    try
                    {
                        if (!type.isAssignableFrom(Byte.TYPE) && !type.isAssignableFrom(Byte.class))
                        {
                            if (!type.isAssignableFrom(Short.TYPE) && !type.isAssignableFrom(Short.class))
                            {
                                if (!type.isAssignableFrom(Integer.TYPE) && !type.isAssignableFrom(Integer.class))
                                {
                                    if (!type.isAssignableFrom(Long.TYPE) && !type.isAssignableFrom(Long.class))
                                    {
                                        if (!type.isAssignableFrom(Float.TYPE) && !type.isAssignableFrom(Float.class))
                                        {
                                            if (!type.isAssignableFrom(Double.TYPE) && !type.isAssignableFrom(Double.class))
                                            {
                                                throw new IllegalArgumentException("Type \'" + type.getSimpleName() + "\' is not a string, enumeration, or primitve type.");
                                            }
                                            else
                                            {
                                                return Double.valueOf(Double.parseDouble(value));
                                            }
                                        }
                                        else
                                        {
                                            return Float.valueOf(Float.parseFloat(value));
                                        }
                                    }
                                    else
                                    {
                                        return Long.decode(value);
                                    }
                                }
                                else
                                {
                                    return Integer.decode(value);
                                }
                            }
                            else
                            {
                                return Short.decode(value);
                            }
                        }
                        else
                        {
                            return Byte.decode(value);
                        }
                    }
                    catch (NumberFormatException var6)
                    {
                        throw new IllegalArgumentException("Invalid numerical value \'" + value + "\'", var6);
                    }
                }
                else
                {
                    return Boolean.valueOf(Boolean.parseBoolean(value));
                }
            }
            else
            {
                return value.length() == 0 ? Character.valueOf('\u0000') : Character.valueOf(value.charAt(0));
            }
        }
        else
        {
            return null;
        }
    }

    public static Number convertNumber(Class<? extends Number> type, Number value) throws IllegalArgumentException
    {
        if (type != null && value != null)
        {
            if (!type.isAssignableFrom(Byte.TYPE) && !type.isAssignableFrom(Byte.class))
            {
                if (!type.isAssignableFrom(Short.TYPE) && !type.isAssignableFrom(Short.class))
                {
                    if (!type.isAssignableFrom(Integer.TYPE) && !type.isAssignableFrom(Integer.class))
                    {
                        if (!type.isAssignableFrom(Long.TYPE) && !type.isAssignableFrom(Long.class))
                        {
                            if (!type.isAssignableFrom(Float.TYPE) && !type.isAssignableFrom(Float.class))
                            {
                                if (!type.isAssignableFrom(Double.TYPE) && !type.isAssignableFrom(Double.class))
                                {
                                    throw new IllegalArgumentException("Type \'" + type.getSimpleName() + "\' is not a numeric type.");
                                }
                                else
                                {
                                    return Double.valueOf(value.doubleValue());
                                }
                            }
                            else
                            {
                                return Float.valueOf(value.floatValue());
                            }
                        }
                        else
                        {
                            return Long.valueOf(value.longValue());
                        }
                    }
                    else
                    {
                        return Integer.valueOf(value.intValue());
                    }
                }
                else
                {
                    return Short.valueOf(value.shortValue());
                }
            }
            else
            {
                return Byte.valueOf(value.byteValue());
            }
        }
        else
        {
            return null;
        }
    }

    public static void addDistributionType(String distributionName, IValidatorFactory validatorFactory)
    {
        if (distributionValidators.containsKey(distributionName))
        {
            throw new IllegalArgumentException("A distribution with the name \'" + distributionName + "\' already exists.");
        }
        else
        {
            distributionValidators.put(distributionName, validatorFactory);
        }
    }

    public static void addDistributionType(String distributionName, IDistributionFactory distFactory)
    {
        addDistributionType(distributionName, (IValidatorFactory)(new ValidatorDistribution.Factory(distFactory)));
    }

    public static void addDistributionType(String distributionName, Class clazz, boolean canGenerate)
    {
        addDistributionType(distributionName, (IDistributionFactory)(new StdDistFactory(clazz, canGenerate)));
    }

    static
    {
        addDistributionType("StandardGen", MapGenClusters.class, true);
        addDistributionType("StandardGenPreset", MapGenClusters.class, false);
        addDistributionType("Veins", MapGenVeins.class, true);
        addDistributionType("VeinsPreset", MapGenVeins.class, false);
        addDistributionType("Cloud", MapGenCloud.class, true);
        addDistributionType("CloudPreset", MapGenCloud.class, false);
        addDistributionType("Substitute", WorldGenSubstitution.class, true);
        addDistributionType("SubstitutePreset", WorldGenSubstitution.class, false);
    }
    
    public class ConfigExpressionEvaluator extends ExpressionEvaluator
    {
        private Map localIdentifiers;

        public ConfigExpressionEvaluator()
        {
            this.localIdentifiers = new CIStringMap();
            this.localIdentifiers.put("isModInstalled", new EvaluationDelegate(false, ModLoader.class, "isModLoaded", new Class[] {String.class}));
            this.localIdentifiers.put("blockExists", new EvaluationDelegate(false, ConfigParser.this, "blockExists", new Class[] {String.class}));
            this.localIdentifiers.put("biomeExists", new EvaluationDelegate(false, ConfigParser.this, "biomeExists", new Class[] {String.class}));
            this.localIdentifiers.put("world.nextRandom", new EvaluationDelegate(false, ConfigParser.this, "nextRandom", new Class[0]));
        }

        public ConfigExpressionEvaluator(Object defaultValue)
        {
            this.localIdentifiers = new CIStringMap();
            this.localIdentifiers.put("_default_", defaultValue);
        }

        protected Object getIdentifierValue(String identifier)
        {
            String lkey = identifier.toLowerCase();
            ConfigOption option = target.getConfigOption(identifier);

            if (option != null)
            {
                return option.getValue();
            }
            else
            {
                Object property = target.getWorldProperty(identifier);

                if (property != null)
                {
                    return property;
                }
                else
                {
                    Object value = this.localIdentifiers.get(identifier);
                    return value != null ? value : (lkey.startsWith("age.") ? Integer.valueOf(0) : super.getIdentifierValue(identifier));
                }
            }
        }
    }

    private static class StdDistFactory implements IDistributionFactory
    {
        protected Constructor _ctor;
        protected boolean _canGen;

        public StdDistFactory(Class clazz, boolean canGenerate)
        {
            try
            {
                this._ctor = clazz.getConstructor(new Class[] {Integer.TYPE, Boolean.TYPE});
            }
            catch (NoSuchMethodException var4)
            {
                throw new IllegalArgumentException(var4);
            }

            this._canGen = canGenerate;
        }

        public IOreDistribution createDistribution(int distributionID)
        {
            try
            {
                return (IOreDistribution)this._ctor.newInstance(new Object[] {Integer.valueOf(distributionID), Boolean.valueOf(this._canGen)});
            }
            catch (InvocationTargetException var3)
            {
                throw new IllegalArgumentException(var3);
            }
            catch (IllegalAccessException var4)
            {
                throw new IllegalArgumentException(var4);
            }
            catch (InstantiationException var5)
            {
                throw new IllegalArgumentException(var5);
            }
        }
    }

}
