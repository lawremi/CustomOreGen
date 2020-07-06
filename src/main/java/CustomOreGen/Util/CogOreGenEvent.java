package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.world.ChunkEvent;
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
    private final Random rand;
    
    public CogOreGenEvent(IChunk chunk, Random rand) {
    	super(chunk);
        this.rand = rand;
    }

    public Random getRand()
    {
        return rand;
    }
}
