package CustomOreGen;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class GeometryRequestData implements Serializable
{
    public transient World world;
    public int dimensionID;
    public int chunkX;
    public int chunkZ;
    public int batchID;
    private static final long serialVersionUID = 2L;

    public GeometryRequestData() {}

    public GeometryRequestData(World world, int chunkX, int chunkZ, int batchID)
    {
        this.world = world;
        this.dimensionID = world == null ? 0 : world.getDimension().getType().getId();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.batchID = batchID;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        MinecraftServer ms = ServerLifecycleHooks.getCurrentServer();

        if (ms != null && ms.isServerRunning())
        {
            this.world = ms.getWorld(DimensionType.getById(dimensionID));
        }
    }
}
