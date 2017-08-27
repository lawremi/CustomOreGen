package CustomOreGen.Util;

import java.util.Random;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;

public class CogOreGenEvent extends OreGenEvent {
	/**
     * CogOreGenEvent is fired just after a chunk is populated with ores by COG.<br>
     * This event is fired just after ore generation in 
     * ServerState#onPopulateChunk().<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#ORE_GEN_BUS}.<br>
     **/ 
    public CogOreGenEvent(World world, Random rand, BlockPos pos) {
        super(world, rand, pos);
    }
}
