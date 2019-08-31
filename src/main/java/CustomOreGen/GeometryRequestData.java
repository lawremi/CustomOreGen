package CustomOreGen;

import java.io.Serializable;

import net.minecraft.world.World;

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
        this.dimensionID = world == null ? 0 : world.dimension.getType().getId();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.batchID = batchID;
    }
}
