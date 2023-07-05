package CustomOreGen.Util;

import net.minecraft.util.RandomSource;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * CogOreGenEvent is fired just after a chunk is populated with ores by COG.<br>
 * This event is fired {TODO}<br>
 * <br>
 * This event is not {@link Cancelable}.<br>
 * <br>
 * This event does not have a result. {@link HasResult} <br>
 **/ 
public class CogOreGenEvent extends ChunkEvent {
    private final RandomSource rand;
    
    public CogOreGenEvent(ChunkAccess chunk, RandomSource rand) {
    	super(chunk);
        this.rand = rand;
    }

    public RandomSource getRand()
    {
        return rand;
    }
}
