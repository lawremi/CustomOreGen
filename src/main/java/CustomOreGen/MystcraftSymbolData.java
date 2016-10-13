package CustomOreGen;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class MystcraftSymbolData implements Serializable
{
    public transient World world;
    public int dimensionID = 0;
    public String symbolName;
    public int count = 0;
    public String displayName = "";
    public float weight = 1.0F;
    public float instability = 0.0F;
    private static final long serialVersionUID = 1L;

    public MystcraftSymbolData() {}

    public MystcraftSymbolData(World world, String symbolName)
    {
        this.world = world;

        if (world != null)
        {
            this.dimensionID = world.provider.getDimension();
        }

        this.symbolName = this.displayName = symbolName;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        MinecraftServer ms = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (ms != null && ms.isServerRunning())
        {
            this.world = ms.worldServerForDimension(this.dimensionID);
        }
    }
}
