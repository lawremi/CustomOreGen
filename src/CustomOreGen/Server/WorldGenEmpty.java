package CustomOreGen.Server;

import java.util.Random;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenEmpty extends WorldGenerator
{
    public static final boolean cacheState = false;
    private final WorldGenerator delegateGenerator;
    private World _lastWorld = null;
    private boolean _lastEnabled = false;

    public WorldGenEmpty(WorldGenerator delegate)
    {
        this.delegateGenerator = delegate;
    }

    public boolean generate(World world, Random rand, int x, int y, int z)
    {
        if (world == this._lastWorld)
        {
            ;
        }

        ServerState.checkIfServerChanged(MinecraftServer.getServer(), world.getWorldInfo());
        this._lastEnabled = ServerState.getWorldConfig(world).vanillaOreGen;
        this._lastWorld = world;
        return this._lastEnabled ? this.delegateGenerator.generate(world, rand, x, y, z) : false;
    }
}
