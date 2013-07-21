package CustomOreGen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.minecraft.world.World;
import xcompwiz.mystcraft.api.symbol.AgeSymbol;
import xcompwiz.mystcraft.api.symbol.DrawableSymbol;
import xcompwiz.mystcraft.api.symbol.IAgeController;
import xcompwiz.mystcraft.api.symbol.IPopulate;
import xcompwiz.mystcraft.api.symbol.ITerrainModifier;
import CustomOreGen.Util.CIStringMap;
import CustomOreGen.Util.MapCollection;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper.UnableToFindMethodException;

public class MystcraftInterface
{
    private static Map cogSymbols = new CIStringMap();

    public static void init() throws NoSuchMethodException, IllegalAccessException
    {
        CustomOreGenBase.log.finer("Initializing Mystcraft interface ...");
        MystcraftAge.init();
        MystcraftSymbolManager.init();
    }

    public static SymbolHandler addCOGSymbol(MystcraftSymbolData symbolData)
    {
        SymbolHandler handler = new SymbolHandler(symbolData);
        Object symbol = MystcraftSymbolManager.createSymbol(handler);
        SymbolHandler oldHandler = (SymbolHandler)cogSymbols.remove(handler.getName());

        if (oldHandler != null)
        {
        	// FIXME: getSymbol() is missing
            //Object oldSymbol = MystcraftSymbolManager.getSymbol(oldHandler.getName());
            //MystcraftSymbolManager.unregisterSymbol(oldSymbol);
        }

        MystcraftSymbolManager.registerSymbol(symbol);
        cogSymbols.put(handler.getName(), handler);
        return handler;
    }

    public static void appyAgeSpecificCOGSymbol(MystcraftSymbolData ageSpecificData) throws IllegalArgumentException
    {
        SymbolHandler handler = (SymbolHandler)cogSymbols.get(ageSpecificData.symbolName);

        if (handler == null)
        {
            throw new IllegalArgumentException("COG Mystcraft Symbol \'" + ageSpecificData.symbolName + "\' is not defined in the world config file.");
        }
        else
        {
            MystcraftAge age = new MystcraftAge(ageSpecificData.world);

            if (age.isAgeValid())
            {
                CustomOreGenBase.log.finest("Merging symbol \'" + ageSpecificData.symbolName + "\' (" + "Dimension = " + ageSpecificData.dimensionID + ", " + "Count = " + ageSpecificData.count + ", " + "Instability = " + Math.round(ageSpecificData.instability) + ") ...");
            }

            String ageProp = null;
            String worldProp = null;

            try
            {
                ageProp = handler.getName();
                worldProp = ageSpecificData.symbolName;

                if (worldProp != ageProp && (worldProp == null || !worldProp.equals(ageProp)))
                {
                    throw new Exception("name");
                }

                ageProp = handler.getDisplayName();
                worldProp = ageSpecificData.displayName;

                if (worldProp != ageProp && (worldProp == null || !worldProp.equals(ageProp)))
                {
                    throw new Exception("display name");
                }

                Float ageProp1 = Float.valueOf(handler.getWeight());
                Float worldProp1 = Float.valueOf(ageSpecificData.weight);

                if (worldProp1 != ageProp1 && (worldProp1 == null || !worldProp1.equals(ageProp1)))
                {
                    throw new Exception("weight");
                }
            }
            catch (Exception var6)
            {
                throw new IllegalArgumentException("The " + var6.getMessage() + " \'" + ageProp + "\' " + "of COG Mystcraft symbol \'" + handler.getName() + "\' in dimension " + ageSpecificData.dimensionID + " does not match the established value \'" + worldProp + "\' for this world." + "This attribute must be the same for all dimensions.");
            }

            age.addInstability(Math.round(ageSpecificData.instability));
        }
    }

    public static void clearCOGSymbols()
    {
        Iterator i$ = cogSymbols.values().iterator();

        while (i$.hasNext())
        {
            SymbolHandler handler = (SymbolHandler)i$.next();
            // FIXME: getSymbol() is missing
            //Object symbol = MystcraftSymbolManager.getSymbol(handler.getName());
            //MystcraftSymbolManager.unregisterSymbol(symbol);
        }

        cogSymbols.clear();
    }

    public static void populateAgePropertyMap(World world, Map ageProperties)
    {
        MystcraftAge age = new MystcraftAge(world);
        ageProperties.put("age", Boolean.valueOf(age.isAgeValid()));
        Iterator i$ = age.getAgeSymbolCounts().entrySet().iterator();

        while (i$.hasNext())
        {
            Entry entry = (Entry)i$.next();
            String key = "age." + (String)entry.getKey();
            ageProperties.put(key, entry.getValue());
        }
    }

    private static Method findOverloadedMethod(Class clazz, String name, Class[] ... args)
    {
        Exception failed = null;
        Class[][] arr$ = args;
        int len$ = args.length;
        int i$ = 0;

        while (i$ < len$)
        {
            Class[] argTypes = arr$[i$];

            try
            {
                Method e = clazz.getDeclaredMethod(name, argTypes);
                e.setAccessible(true);
                return e;
            }
            catch (Exception var9)
            {
                failed = var9;
                ++i$;
            }
        }

        throw new UnableToFindMethodException(new String[] {name}, failed);
    }

    private static Collection getAbstractMethods(Class clazz)
    {
        if (clazz == null)
        {
            return new LinkedList();
        }
        else
        {
            MapCollection<Collection,Method> abstractMethods = 
            		new MapCollection<Collection,Method>(new HashMap()) {
            	protected Collection getKey(Method method)
                {
                    ArrayList key = new ArrayList(method.getParameterTypes().length + 2);
                    key.add(method.getName());
                    key.add(method.getReturnType());
                    Collections.addAll(key, method.getParameterTypes());
                    return key;
                }
            };

            if (clazz.getSuperclass() != null)
            {
                abstractMethods.addAll(getAbstractMethods(clazz.getSuperclass()));
            }

            if (clazz.getInterfaces() != null) {
                for (Class iface : clazz.getInterfaces()) {
                	abstractMethods.addAll(getAbstractMethods(iface));
                }
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (!clazz.isInterface() && !Modifier.isAbstract(method.getModifiers()))
                {
                    abstractMethods.remove(method);
                }
                else
                {
                    abstractMethods.add(method);
                }
            }

            return abstractMethods;
        }
    }

    private static class OldSymbol extends AgeSymbol implements IPopulate, ITerrainModifier
    {
        private final SymbolHandler _handler;

        public OldSymbol(SymbolHandler handler)
        {
            this._handler = handler;
        }

        public void affectTerrain(World worldObj, int chunkX, int chunkZ, short[] blocks, byte[] metadata) {}

        public void affectTerrain(World worldObj, int chunkX, int chunkZ, byte[] metadata) {}

        public boolean populate(World world, Random rand, int chunkX, int chunkZ, boolean flag)
        {
            return false;
        }

        public void registerLogic(IAgeController controller, long seed)
        {
            this._handler.registerLogic(this, new MystcraftAge(controller));
        }

        public String identifier()
        {
            return this._handler.getName();
        }

        public Category getCategory()
        {
            Category[] arr$ = Category.values();
            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$)
            {
                Category category = arr$[i$];

                if (this._handler.getCategory().equals(category.toString()))
                {
                    return category;
                }
            }

            return null;
        }

        public String[] getDescriptorWords()
        {
            return new String[] {this._handler.getCategory(), null, null, null};
        }

        public int instabilityModifier(int var1)
        {
            return Integer.valueOf(0).intValue();
        }

        public float getRarity()
        {
            return this._handler.getWeight();
        }

        public AgeSymbol setSymbolRarity(float rarity)
        {
            this._handler.setWeight(rarity);
            return this;
        }

        public String displayName()
        {
            return this._handler.getDisplayName();
        }

        public DrawableSymbol setDisplayName(String name)
        {
            this._handler.setDisplayName(name);
            return this;
        }

        public boolean equals(Object otherObject)
        {
            return this.compareTo(otherObject) == 0;
        }

        public String toString()
        {
            return this._handler.getName();
        }

    }
    
    private static class SymbolHandler implements InvocationHandler
    {
        private final MystcraftSymbolData data;

        public SymbolHandler(MystcraftSymbolData data)
        {
            this.data = data;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            try
            {
                if (method.getName().equals("populate"))
                {
                    return Boolean.FALSE;
                }
                else if (method.getName().equals("affectTerrain"))
                {
                    return null;
                }
                else if (method.getName().equals("registerLogic"))
                {
                    this.registerLogic(proxy, new MystcraftAge(args[0]));
                    return null;
                }
                else if (method.getName().equals("identifier"))
                {
                    return this.getName();
                }
                else if (method.getName().equals("instabilityModifier"))
                {
                    return Integer.valueOf(0);
                }
                else if (method.getName().equals("getRarity"))
                {
                    return Float.valueOf(this.getWeight());
                }
                else if (method.getName().equals("setSymbolRarity"))
                {
                    this.setWeight(((Float)args[0]).floatValue());
                    return proxy;
                }
                else if (method.getName().equals("compareTo"))
                {
                    return Integer.valueOf(this.compareTo(args[0]));
                }
                else if (method.getName().equals("displayName"))
                {
                    return this.getDisplayName();
                }
                else if (method.getName().equals("setDisplayName"))
                {
                    this.setDisplayName((String)args[0]);
                    return proxy;
                }
                else if (method.getName().equals("getCategory"))
                {
                    Class ex = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.AgeSymbol.Category"});
                    Object[] arr$ = ex.getEnumConstants();
                    int len$ = arr$.length;

                    for (int i$ = 0; i$ < len$; ++i$)
                    {
                        Object category = arr$[i$];

                        if (this.getCategory().equals(category.toString()))
                        {
                            return category;
                        }
                    }

                    return null;
                }
                else if (method.getName().equals("getDescriptorWords"))
                {
                    return new String[] {this.getCategory(), null, null, null};
                }
                else if (method.getName().equals("components"))
                {
                    return new ArrayList();
                }
                else if (method.getName().equals("colors"))
                {
                    return new ArrayList();
                }
                else if (method.getName().equals("addDrawComponent"))
                {
                    return proxy;
                }
                else if (method.getName().equals("addDrawWord"))
                {
                    return proxy;
                }
                else if (method.getName().equals("imageSource"))
                {
                    return "/myst/symbolcomponents.png";
                }
                else if (method.getName().equals("setImageSource"))
                {
                    return proxy;
                }
                else if (method.getName().equals("equals"))
                {
                    return Boolean.valueOf(this.equals(args[0]));
                }
                else if (method.getName().equals("hashCode"))
                {
                    return Integer.valueOf(this.hashCode());
                }
                else if (method.getName().equals("toString"))
                {
                    return this.toString();
                }
                else
                {
                    throw new NoSuchMethodException("Unhandled proxy method " + method.getName());
                }
            }
            catch (Exception var9)
            {
                CustomOreGenBase.log.throwing("SymbolHandler", "invoke:" + method.getName(), var9);
                return null;
            }
        }

        public String getName()
        {
            return this.data.symbolName;
        }

        public String getDisplayName()
        {
            return this.data.displayName != null ? this.data.displayName : this.data.symbolName;
        }

        public void setDisplayName(String name)
        {
            this.data.displayName = name;
        }

        public float getWeight()
        {
            return this.data.weight;
        }

        public void setWeight(float weight)
        {
            this.data.weight = weight;
        }

        public String getCategory()
        {
            return "TerrainFeature";
        }

        public void registerLogic(Object parentSymbol, MystcraftAge age)
        {
            age.registerPopulator(parentSymbol);
            age.registerTerrainModifier(parentSymbol);
        }

        public int compareTo(Object otherObject)
        {
            String otherSymbolName = MystcraftSymbolManager.getSymbolName(otherObject);
            return otherSymbolName != null ? this.getDisplayName().compareTo(otherSymbolName) : this.getDisplayName().compareTo(otherObject.toString());
        }

        public boolean equals(Object otherObject)
        {
            return this.compareTo(otherObject) == 0;
        }

        public int hashCode()
        {
            return System.identityHashCode(this);
        }

        public String toString()
        {
            return this.getName();
        }
    }
    
    static class MystcraftSymbolManager
    {
        private static Map globalSymbols;
        private static Class proxySymbolClass;
        private static Class _sm_class;
        private static Method _sm_regSymbol;
        private static Field _sm_symbols;
        private static Class _po_class;
        private static Class _tm_class;
        private static Class _as_class;
        private static Method _as_identifier;

        public static void init() throws NoSuchMethodException, IllegalAccessException
        {
            _as_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.IAgeSymbol", "xcompwiz.mystcraft.api.symbol.AgeSymbol"});
            _as_identifier = MystcraftInterface.findOverloadedMethod(_as_class, "identifier", new Class[][] {new Class[0]});
            _po_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.IPopulate"});
            _tm_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.ITerrainModifier"});
            _sm_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.AgeSymbolManager"});
            _sm_regSymbol = MystcraftInterface.findOverloadedMethod(_sm_class, "registerSymbol", new Class[][] {{_as_class}});
            _sm_symbols = ReflectionHelper.findField(_sm_class, new String[] {"ageSymbols"});
            globalSymbols = (Map)_sm_symbols.get((Object)null);
            globalSymbols.size();

            if (_as_class.isInterface())
            {
                proxySymbolClass = Proxy.getProxyClass(MystcraftInterface.class.getClassLoader(), new Class[] {_as_class, _po_class, _tm_class});
            }
            else
            {
                Collection abstractMethods = MystcraftInterface.getAbstractMethods(OldSymbol.class);

                if (!abstractMethods.isEmpty())
                {
                    throw new NoSuchMethodException(OldSymbol.class.getName() + " does not implement the abstract methods " + abstractMethods);
                }
            }
        }

        public static Object createSymbol(SymbolHandler handler)
        {
            try
            {
                return proxySymbolClass != null ? proxySymbolClass.getConstructor(new Class[] {InvocationHandler.class}).newInstance(new Object[] {handler}): new OldSymbol(handler);
            }
            catch (Exception var2)
            {
                CustomOreGenBase.log.throwing("MystcraftSymbolManager", "createSymbol", var2);
                return null;
            }
        }

        public static String getSymbolName(Object symbol)
        {
            if (symbol != null && _as_class.isInstance(symbol))
            {
                try
                {
                    return (String)_as_identifier.invoke(symbol, new Object[0]);
                }
                catch (Exception var2)
                {
                    CustomOreGenBase.log.throwing("MystcraftSymbolManager", "getSymbolName", var2);
                }
            }

            return null;
        }
        
        public static boolean registerSymbol(Object symbol)
        {
            if (symbol != null && _as_class.isInstance(symbol))
            {
                try
                {
                    _sm_regSymbol.invoke((Object)null, new Object[] {symbol});
                    return true;
                }
                catch (Exception var2)
                {
                    CustomOreGenBase.log.throwing("MystcraftSymbolManager", "registerSymbol", var2);
                }
            }

            return false;
        }

        public static boolean unregisterSymbol(Object symbol)
        {
            if (symbol != null && _as_class.isInstance(symbol))
            {
                try
                {
                    return globalSymbols.values().remove(symbol);
                }
                catch (Exception var2)
                {
                    CustomOreGenBase.log.throwing("MystcraftInterface", "unregisterSymbol", var2);
                }
            }

            return false;
        }

        public static Collection getAllSymbolNames()
        {
            return globalSymbols.keySet();
        }
    }

    static class MystcraftAge
    {
        private final World world;
        private final Object ageController;
        private static Class _wp_class;
        private static Field _wp_controller;
        private static Class _po_class;
        private static Class _tm_class;
        private static Class _ac_class;
        private static Method _ac_regPopulator;
        private static Method _ac_regTerrainMod;
        private static Field _ac_world;
        private static Field _ac_instability;
        private static Field _ac_symbolCounts;

        public static void init()
        {
            _wp_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"WorldProviderMyst", "net.minecraft.src.WorldProviderMyst", "xcompwiz.mystcraft.WorldProviderMyst"});
            _wp_controller = ReflectionHelper.findField(_wp_class, new String[] {"controller"});
            _po_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.IPopulate"});
            _tm_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.api.symbol.ITerrainModifier"});
            _ac_class = ReflectionHelper.getClass(MystcraftInterface.class.getClassLoader(), new String[] {"xcompwiz.mystcraft.AgeController"});
            _ac_regPopulator = MystcraftInterface.findOverloadedMethod(_ac_class, "registerInterface", new Class[][] {{_po_class}});
            _ac_regTerrainMod = MystcraftInterface.findOverloadedMethod(_ac_class, "registerInterface", new Class[][] {{_tm_class}});
            _ac_world = ReflectionHelper.findField(_ac_class, new String[] {"world"});
            _ac_instability = ReflectionHelper.findField(_ac_class, new String[] {"instability"});
            _ac_symbolCounts = ReflectionHelper.findField(_ac_class, new String[] {"symbolcounts"});
        }

        public MystcraftAge(World world)
        {
            this.world = world;
            Object controller = null;

            if (world != null && world.provider != null && _wp_class.isInstance(world.provider))
            {
                try
                {
                    controller = _wp_controller.get(world.provider);
                }
                catch (Exception var4)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "[ctor](world)", var4);
                }
            }

            this.ageController = controller;
        }

        public MystcraftAge(Object ageController)
        {
            this.ageController = ageController;
            World w = null;

            if (this.isAgeValid())
            {
                try
                {
                    w = (World)_ac_world.get(ageController);
                }
                catch (Exception var4)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "[ctor](controller)", var4);
                }
            }

            this.world = w;
        }

        public World getWorld()
        {
            return this.world;
        }

        public boolean isAgeValid()
        {
            return this.ageController != null && _ac_class.isInstance(this.ageController);
        }

        public void registerPopulator(Object populator)
        {
            if (this.isAgeValid() && populator != null && _po_class.isInstance(populator))
            {
                try
                {
                    _ac_regPopulator.invoke(this.ageController, new Object[] {populator});
                }
                catch (Exception var3)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "registerPopulator", var3);
                }
            }
        }

        public void registerTerrainModifier(Object terrainModifier)
        {
            if (this.isAgeValid() && terrainModifier != null && _tm_class.isInstance(terrainModifier))
            {
                try
                {
                    _ac_regTerrainMod.invoke(this.ageController, new Object[] {terrainModifier});
                }
                catch (Exception var3)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "registerTerrainModifier", var3);
                }
            }
        }

        public int addInstability(int instability)
        {
            if (this.isAgeValid())
            {
                try
                {
                    Integer ex = (Integer)_ac_instability.get(this.ageController);

                    if (ex != null)
                    {
                        ex = Integer.valueOf(ex.intValue() + instability);
                        _ac_instability.set(this.ageController, ex);
                        return ex.intValue();
                    }
                }
                catch (Exception var3)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "addInstability", var3);
                }
            }

            return 0;
        }

        public Map getAgeSymbolCounts()
        {
            HashMap counts = new HashMap();
            Iterator symbolCounts = MystcraftSymbolManager.getAllSymbolNames().iterator();

            while (symbolCounts.hasNext())
            {
                String i$ = (String)symbolCounts.next();
                counts.put(i$, Integer.valueOf(0));
            }

            if (!this.isAgeValid())
            {
                return counts;
            }
            else
            {
                Map symbolCounts1 = null;

                try
                {
                    symbolCounts1 = (Map)_ac_symbolCounts.get(this.ageController);
                }
                catch (Exception var6)
                {
                    CustomOreGenBase.log.throwing("MystcraftAge", "getAgeSymbolCounts", var6);
                }

                if (symbolCounts1 == null)
                {
                    return counts;
                }
                else
                {
                    Iterator i = symbolCounts1.entrySet().iterator();

                    while (i.hasNext())
                    {
                        Entry entry = (Entry)i.next();
                        String key = MystcraftSymbolManager.getSymbolName(entry.getKey());

                        if (key != null && entry.getValue() != null && entry.getValue() instanceof Integer)
                        {
                            counts.put(key, entry.getValue());
                        }
                    }

                    return counts;
                }
            }
        }
    }

}
