package CustomOreGen.Server;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * What remains of the pre-1.13 vanilla MapGenStructure that COG was built on. Vanilla's
 * StructureStart can no longer be subclassed or have pieces added to it, so the structure cache
 * and structure groups live in MapGenOreDistribution.
 */
public abstract class MapGenStructure extends MapGenBase
{
    public abstract String getStructureName();

    @Nullable
    public abstract BlockPos getNearestStructurePos(Level worldIn, BlockPos pos, boolean findUnexplored);

    protected abstract boolean canSpawnStructureAtCoords(int chunkX, int chunkZ);
}
