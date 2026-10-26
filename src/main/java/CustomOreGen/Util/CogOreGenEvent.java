package CustomOreGen.Util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * CogOreGenEvent is fired just after a chunk is populated with ores by COG.<br>
 * This event is fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} from a worldgen thread,
 * once per chunk, after every chunk around it has finished its features (so the chunk's ores are final).<br>
 * {@link #getRegion()} is the generating chunk's WorldGenRegion: blocks may be read anywhere in it, but only
 * written within one chunk of {@code getRegion().getCenter()}, which always includes this chunk.<br>
 * <br>
 * This event is not cancellable.<br>
 * <br>
 * This event does not have a result. {@link HasResult} <br>
 **/
public class CogOreGenEvent extends ChunkEvent {
    private final WorldGenLevel region;
    private final RandomSource rand;

    public CogOreGenEvent(WorldGenLevel region, ChunkAccess chunk, RandomSource rand) {
    	super(chunk);
        this.region = region;
        this.rand = rand;
    }

    public WorldGenLevel getRegion()
    {
        return region;
    }

    public RandomSource getRand()
    {
        return rand;
    }
}
