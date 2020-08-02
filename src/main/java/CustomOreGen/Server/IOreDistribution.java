package CustomOreGen.Server;

import java.util.Map;

import CustomOreGen.Util.GeometryStream;
import net.minecraft.world.IWorld;

public interface IOreDistribution
{
    Map<String,String> getDistributionSettingDescriptions();

    Object getDistributionSetting(String name);

    void setDistributionSetting(String name, Object value) throws IllegalArgumentException, IllegalAccessException;

    void generate(IWorld world, int chunkX, int chunkZ);

    void populate(IWorld world, int chunkX, int chunkY);

    void cull();

    void clear();

    GeometryStream getDebuggingGeometry(IWorld world, int chunkX, int chunkY);

    boolean validate() throws IllegalStateException;

    void inheritFrom(IOreDistribution var1) throws IllegalArgumentException;
    
    double getOresPerChunk();
    
    public static interface IDistributionFactory
    {
        IOreDistribution createDistribution(int id);
    }

    public enum StandardSettings
    {
        Name,
        DisplayName,
        Seed,
        OreBlock,
        Replaces,
        PlacesAbove,
        PlacesBelow,
        PlacesBeside,
        Touches,
        TargetBiome,
        Parent;
    }

}
