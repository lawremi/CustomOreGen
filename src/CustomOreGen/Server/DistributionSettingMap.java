package CustomOreGen.Server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import CustomOreGen.Config.ConfigParser;
import CustomOreGen.Util.CIStringMap;

public class DistributionSettingMap
{
    private final Map<String,Object[]> _settingMap = new CIStringMap(new LinkedHashMap());

    public DistributionSettingMap(Class distributionType)
    {
    	for (Field field : distributionType.getFields()) {
    		DistributionSetting s = (DistributionSetting)field.getAnnotation(DistributionSetting.class);

            if (s != null)
            {
                this._settingMap.put(s.name(), new Object[] {field, s});
            }
        }
    }

    public Map<String,String> getDescriptions()
    {
        CIStringMap<String> descriptions = new CIStringMap(new LinkedHashMap());
        
        for (Entry<String,Object[]> entry : this._settingMap.entrySet()) {
        	DistributionSetting s = (DistributionSetting)(entry.getValue())[1];
            descriptions.put(s.name(), s.info());
        }

        return descriptions;
    }

    public String getDescription(String settingName)
    {
        Object[] entry = this._settingMap.get(settingName);
        return entry == null ? null : ((DistributionSetting)entry[1]).info();
    }

    public Object get(IOreDistribution dist, String settingName)
    {
        if (dist == null)
        {
            return null;
        }
        else
        {
            Object[] entry = this._settingMap.get(settingName);

            if (entry == null)
            {
                return null;
            }
            else
            {
                Field field = (Field)entry[0];

                if (field == null)
                {
                    return null;
                }
                else
                {
                    try
                    {
                        return field.get(dist);
                    }
                    catch (IllegalAccessException var6)
                    {
                        return null;
                    }
                    catch (ClassCastException var7)
                    {
                        return null;
                    }
                }
            }
        }
    }

    public void set(IOreDistribution dist, String settingName, Object value) throws IllegalArgumentException, IllegalAccessException
    {
        if (dist != null)
        {
            Object[] entry = (Object[])this._settingMap.get(settingName);

            if (entry == null)
            {
                throw new IllegalArgumentException("Setting \'" + settingName + "\' is not supported by distribution \'" + dist + "\'");
            }
            else
            {
                Field field = (Field)entry[0];
                DistributionSetting annotation = (DistributionSetting)entry[1];

                if (value != null && value instanceof String)
                {
                    value = ConfigParser.parseString(field.getType(), (String)value);
                }

                if (Modifier.isFinal(field.getModifiers()))
                {
                    try
                    {
                        Object ex = field.get(dist);

                        if (!Copyable.class.isAssignableFrom(field.getType()))
                        {
                            throw new IllegalStateException("Setting is final and does not support copying");
                        }

                        if (ex == null || value == null)
                        {
                            throw new IllegalStateException("Setting is final and null");
                        }

                        ((Copyable)ex).copyFrom(value);
                    }
                    catch (Exception var8)
                    {
                        throw new IllegalArgumentException("Failed to copy setting \'" + annotation.name() + "\' for distribution \'" + dist + "\'", var8);
                    }
                }
                else
                {
                    field.set(dist, value);
                }
            }
        }
    }

    public void inheritAll(IOreDistribution source, IOreDistribution destination)
    {
    	for (Entry<String,Object[]> entry : _settingMap.entrySet()) {
            Field field = (Field)(entry.getValue())[0];
            DistributionSetting annotation = (DistributionSetting)(entry.getValue())[1];

            try
            {
                if (annotation.inherited())
                {
                    Object ex = field.get(source);

                    if (Modifier.isFinal(field.getModifiers()))
                    {
                        Object dstVal = field.get(destination);

                        if (!Copyable.class.isAssignableFrom(field.getType()))
                        {
                            throw new IllegalStateException("Setting is final and does not support copying");
                        }

                        if (ex == null || dstVal == null)
                        {
                            throw new IllegalStateException("Setting is null");
                        }

                        ((Copyable)dstVal).copyFrom(ex);
                    }
                    else
                    {
                        field.set(destination, ex);
                    }
                }
            }
            catch (Exception var9)
            {
                ;
            }
        }
    }
    
    public interface Copyable<T>
    {
        void copyFrom(T var1);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target( {ElementType.FIELD})
    public @interface DistributionSetting
    {
        String name();
        String info() default "";

        boolean inherited() default true;
    }


}
