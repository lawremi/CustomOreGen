package CustomOreGen.Config;

import CustomOreGen.Util.BlockDescriptor.BlockInfo;
import net.minecraft.util.math.BlockPos;

public class BlockAndPosition {
    public BlockInfo block;
    public BlockPos position;
    
    public BlockAndPosition(BlockInfo block, BlockPos position) {
        this.block = block;
        this.position = position;
    }
}
