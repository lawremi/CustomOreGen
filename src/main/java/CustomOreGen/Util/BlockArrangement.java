package CustomOreGen.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class BlockArrangement {
    private BlockDescriptor center, above, below, beside;
    private TouchingDescriptorList touching;

    public BlockArrangement(BlockDescriptor center, BlockDescriptor above, BlockDescriptor below,
            BlockDescriptor beside, TouchingDescriptorList touching) {
        super();
        this.center = center;
        this.above = above;
        this.below = below;
        this.beside = beside;
        this.touching = touching;
    }

    public boolean matchesAt(LevelAccessor world, RandomSource rand, BlockPos pos) {
        return 
            this.descriptorMatchesAt(center, world, rand, pos) &&
            this.descriptorMatchesAt(above, world, rand, pos.above()) &&
            this.descriptorMatchesAt(below, world, rand, pos.below()) && 
            
            (this.descriptorMatchesAt(beside, world, rand, pos.north()) ||
            this.descriptorMatchesAt(beside, world, rand, pos.east()) ||
            this.descriptorMatchesAt(beside, world, rand, pos.south()) ||
            this.descriptorMatchesAt(beside, world, rand, pos.west())) &&
            
            this.touchesAt(world, rand, pos);
    }

    private boolean descriptorMatchesAt(BlockDescriptor descriptor, LevelAccessor world, RandomSource rand, BlockPos pos) {
        if (descriptor.isEmpty()) {
            return true;
        }
        BlockState blockState = world.getBlockState(pos);
        return descriptor.matchesBlock(blockState, rand);
    }

    private boolean touchesAt(LevelAccessor world, RandomSource rand, BlockPos pos) {
        if (this.touching.size() <= 0)
            return true;

        // mandatory touching descriptors, all must pass
        for (TouchingDescriptor descriptor : this.touching) {
            if (descriptor.mandatory) {
                if (!touchesAt(descriptor, world, rand, pos)) {
                    // mandatory descriptor failed, can't place block
                    return false;
                }
            }
        }

        // optional touching descriptors, at least one must pass
        int numberOfOptionalDescriptors = 0;
        for (TouchingDescriptor descriptor : this.touching) {
            if (!descriptor.mandatory) {
                numberOfOptionalDescriptors++;

                if (touchesAt(descriptor, world, rand, pos)) {
                    // one passed
                    return true;
                }
            }
        }

        if (numberOfOptionalDescriptors <= 0) {
            // all mandatory passed and there are no optional
            return true;
        }

        // no optionals passed or there are no touching rules for this distribution
        return false;
    }

    private boolean touchesAt(TouchingDescriptor descriptor, LevelAccessor world, RandomSource rand, BlockPos pos) {
        int numberOfTouches = 0;

        // search each required block position
        for (BlockPos positionDelta : descriptor.deltaPositionMap) {
            if (descriptorMatchesAt(descriptor.blockDescriptor, world, rand, pos.offset(positionDelta))) {
                numberOfTouches++;
            }
        }

        boolean returnValue = true;

        if (numberOfTouches < descriptor.minimumTouches || numberOfTouches > descriptor.maximumTouches)
            returnValue = false;

        if (descriptor.negate) {
            returnValue = !returnValue;
        }

        return returnValue;
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

    public TouchingDescriptorList getTouching() {
        return touching;
    }
}
