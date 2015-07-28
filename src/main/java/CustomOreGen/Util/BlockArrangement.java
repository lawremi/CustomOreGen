package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.block.Block;
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
	
	public boolean matchesAt(World world, Random rand, int x, int y, int z) {
		return 
			this.descriptorMatchesAt(center, world, rand, x, y, z) &&
			this.descriptorMatchesAt(above, world, rand, x, y + 1, z) &&
			this.descriptorMatchesAt(below, world, rand, x, y - 1, z) && 
			(this.descriptorMatchesAt(beside, world, rand, x + 1, y, z) ||
			 this.descriptorMatchesAt(beside, world, rand, x, y, z + 1) ||
			 this.descriptorMatchesAt(beside, world, rand, x - 1, y, z) ||
			 this.descriptorMatchesAt(beside, world, rand, x, y, z - 1));
	}

	private boolean descriptorMatchesAt(BlockDescriptor descriptor, World world,
			Random rand, int x, int y, int z) {
		if (descriptor.isEmpty()) {
			return true;
		}
		Block block = world.getBlock(x, y, z);
		int fastCheck = descriptor.matchesBlock_fast(block);
		if (fastCheck == -1) {
			return descriptor.matchesBlock(block, world.getBlockMetadata(x, y, z), rand);
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
