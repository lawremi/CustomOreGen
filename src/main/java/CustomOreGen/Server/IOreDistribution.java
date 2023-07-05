package CustomOreGen.Server;

import java.util.Map;

import CustomOreGen.Util.GeometryStream;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;

public interface IOreDistribution
{
    Map<String,String> getDistributionSettingDescriptions();

    Object getDistributionSetting(String name);

    void setDistributionSetting(String name, Object value) throws IllegalArgumentException, IllegalAccessException;

    void generate(LevelAccessor world, int chunkX, int chunkZ);

    void populate(WorldGenLevel world, int chunkX, int chunkY);

    void cull();

    void clear();

    GeometryStream getDebuggingGeometry(LevelAccessor world, int chunkX, int chunkY);

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
