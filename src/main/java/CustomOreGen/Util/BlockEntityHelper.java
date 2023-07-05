package CustomOreGen.Util;

import java.util.Iterator;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unchecked")
public class BlockEntityHelper {

	public static void readFromPartialNBT(BlockEntity te, CompoundTag source) {
		CompoundTag dest = te.saveWithFullMetadata();
		mergeNbt(source, dest);
		te.load(dest);
	}

	public static void readFromPartialNBT(LevelAccessor world, int x, int y, int z, CompoundTag source) {
		if (source != null) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockEntity te = world.getBlockEntity(pos);
			if (te == null) {
				//te = tryToCreateGTPrefixBlockBlockEntity(world.getBlockState(pos).getBlock());
				//if (te != null) {
				//	world.setBlockEntity(te);
				//}
			}
			if (te != null) {
				BlockEntityHelper.readFromPartialNBT(te, source);
				BlockState state = world.getBlockState(pos);
				world.blockUpdated(pos, state.getBlock());
			}
		}
	}

	static private Class<? extends Block> prefixBlockClass;
	static private Class<? extends BlockEntity> prefixBlockBlockEntityClass;

	static {
		try {
			prefixBlockClass = (Class<? extends Block>) Class.forName("gregapi.block.prefixblock.PrefixBlock");
			prefixBlockBlockEntityClass = (Class<? extends BlockEntity>) Class.forName("gregapi.block.prefixblock.PrefixBlockBlockEntity");
		} catch (ClassNotFoundException e) {
		}
	}

	public static CompoundTag tryToCreateGTPrefixBlockNBT(ItemStack ore) {
		Block block = ((BlockItem)ore.getItem()).getBlock();
		CompoundTag nbt = null;
		if (isGTPrefixBlock(block)) {
			nbt = new CompoundTag();
			nbt.putString("id", "gt.MetaBlockBlockEntity");
		}
		return nbt;
	}

	private static boolean isGTPrefixBlock(Block block) {
		return prefixBlockClass != null && prefixBlockClass.isAssignableFrom(block.getClass());
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private static BlockEntity tryToCreateGTPrefixBlockBlockEntity(Block block) {
		try {
			if (isGTPrefixBlock(block)) {
				return prefixBlockBlockEntityClass.newInstance();
			}
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}

	private static void mergeNbt(CompoundTag source, CompoundTag dest) {
		Iterator<String> keys = ((Set<String>)source.getAllKeys()).iterator(); 
		while(keys.hasNext()) {
			String key = keys.next();
			dest.put(key, source.getCompound(key));
		}
	}

}
