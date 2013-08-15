package CustomOreGen;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import net.minecraft.src.ModLoader;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.WorldConfig;
import cpw.mods.fml.common.Loader;

public class CustomOreGenBase
{
    public static final String version = "@VERSION@";
    public static final String mcVersion = "@MCVERSION@";
    public static Logger log = Logger.getLogger("STDOUT");
    private static int _hasFML = 0;
    private static int _hasForge = 0;
    private static int _hasMystcraft = 0;

    public static boolean isClassLoaded(String className)
    {
        try
        {
            CustomOreGenBase.class.getClassLoader().loadClass(className);
            return true;
        }
        catch (ClassNotFoundException var2)
        {
            return false;
        }
    }

    public static void onModPostLoad()
    {
        ConsoleCommands.createAndRegister();
        File configPath = Loader.instance().getConfigDir();

        unpackResourceFile("CustomOreGen_Config.xml", new File(configPath, "CustomOreGen_Config.xml"));
        
        File cfg = new File(configPath, "CustomOreGen Standard Modules");
        cfg.mkdir();
        String[] ex = new String[] {"ExtraCaves.xml", "MinecraftOres.xml", "IndustrialCraft2.xml", "Forestry.xml", "Redpower2.xml"};
        String[] extraModules = ex;
        for (String module : extraModules) {
        	unpackResourceFile("CustomOreGen Standard Modules/" + module, new File(cfg, module));
        }
        
        File var9 = new File(configPath, "CustomOreGen Extra Modules");
        var9.mkdir();

        hasMystcraft();
        WorldConfig var8 = null;

        while (var8 == null)
        {
            try
            {
                var8 = new WorldConfig();
            }
            catch (Exception var7)
            {
                if (!ServerState.onConfigError(var7))
                {
                    break;
                }

                var8 = null;
            }
        }
    }

    public static File unpackStandardModule(String moduleName)
    {
        File file = new File(Loader.instance().getConfigDir(), "CustomOreGen Standard Modules" + moduleName);

        if (!file.exists())
        {
            unpackResourceFile("CustomOreGen Standard Modules/" + moduleName, file);
        }

        return file;
    }

    public static boolean unpackResourceFile(String resourceName, File destination)
    {
        if (destination.exists())
        {
            return false;
        }
        else
        {
            try
            {
                log.fine("Unpacking \'" + resourceName + "\' ...");
                InputStream ex = CustomOreGenBase.class.getClassLoader().getResourceAsStream(resourceName);
                BufferedOutputStream streamOut = new BufferedOutputStream(new FileOutputStream(destination));
                byte[] buffer = new byte[1024];
                boolean len = false;
                int len1;

                while ((len1 = ex.read(buffer)) >= 0)
                {
                    streamOut.write(buffer, 0, len1);
                }

                ex.close();
                streamOut.close();
                return true;
            }
            catch (Exception var6)
            {
                throw new RuntimeException("Failed to unpack resource \'" + resourceName + "\'", var6);
            }
        }
    }

    public static boolean hasFML()
    {
        if (_hasFML == 0)
        {
            _hasFML = isClassLoaded("cpw.mods.fml.common.FMLCommonHandler") ? 1 : -1;
        }

        return _hasFML == 1;
    }

    public static boolean hasForge()
    {
        if (_hasForge == 0)
        {
            _hasForge = isClassLoaded("net.minecraftforge.common.MinecraftForge") ? 1 : -1;
        }

        return _hasForge == 1;
    }

    public static boolean hasMystcraft()
    {
        if (_hasMystcraft == 0)
        {
            try
            {
                _hasMystcraft = -1;

                if (ModLoader.isModLoaded("Mystcraft"))
                {
                	/* FIXME: re-enable after restoring Mystcraft compatibility
                    MystcraftInterface.init();
                    _hasMystcraft = 1;
                    */
                }
            }
            catch (Throwable var1)
            {
                log.severe("COG Mystcraft interface appears to be incompatible with the installed version of Mystcraft.");
                log.throwing("MystcraftInterface", "checkInterface", var1);
            }
        }

        return _hasMystcraft == 1;
    }
}
