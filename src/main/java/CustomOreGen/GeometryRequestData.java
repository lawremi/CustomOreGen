package CustomOreGen;

import java.io.Serializable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class GeometryRequestData implements Serializable
{
    public transient Level world;
    public ResourceLocation dimensionID;
    public int chunkX;
    public int chunkZ;
    public int batchID;
    private static final long serialVersionUID = 2L;

    public GeometryRequestData() {}

    public GeometryRequestData(Level world, int chunkX, int chunkZ, int batchID)
    {
        this.world = world;
        this.dimensionID = world == null ? null : world.dimension().location();
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.batchID = batchID;
    }
}
