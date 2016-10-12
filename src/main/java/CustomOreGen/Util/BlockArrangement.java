package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockArrangement {
	private BlockDescriptor center, above, below, beside;

	public BlockArrangement(BlockDescriptor center, BlockDescriptor above,
			BlockDescriptor below, BlockDescriptor beside) {
		super();
		this.center = center;
		this.above = above;
		this.below = below;
		this.beside = beside;
	}
	
	public boolean matchesAt(World world, Random rand, BlockPos pos) {
		return 
			this.descriptorMatchesAt(center, world, rand, pos) &&
			this.descriptorMatchesAt(above, world, rand, pos.up()) &&
			this.descriptorMatchesAt(below, world, rand, pos.down()) && 
			(this.descriptorMatchesAt(beside, world, rand, pos.east()) ||
			 this.descriptorMatchesAt(beside, world, rand, pos.south()) ||
			 this.descriptorMatchesAt(beside, world, rand, pos.west()) ||
			 this.descriptorMatchesAt(beside, world, rand, pos.north()));
	}

	private boolean descriptorMatchesAt(BlockDescriptor descriptor, World world,
			Random rand, BlockPos pos) {
		if (descriptor.isEmpty()) {
			return true;
		}
		IBlockState blockState = world.getBlockState(pos);
		int fastCheck = descriptor.matchesBlock_fast(blockState);
		if (fastCheck == -1) {
			return descriptor.matchesBlock(blockState, rand);
		}
		return fastCheck == 1;
	}
	
	public BlockDescriptor getCenter() {
		return center;
	}

	public BlockDescriptor getAbove() {
		return above;
	}

	public BlockDescriptor getBelow() {
		return below;
	}

	public BlockDescriptor getBeside() {
		return beside;
	}

}
