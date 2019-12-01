package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

public class CogOreGenEvent extends BlockEvent {
	Random random;
	/**
     * CogOreGenEvent is fired just after a chunk is populated with ores by COG.
     * The state does not contain meaningful information.<br>
     * This event is fired just after ore generation in 
     * ServerState#onPopulateChunk().<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     **/ 
    public CogOreGenEvent(World world, Random rand, BlockPos pos) {
        super(world, pos, Blocks.AIR.getDefaultState());
        random = rand;
    }
}
