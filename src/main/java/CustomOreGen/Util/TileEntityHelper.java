package CustomOreGen.Util;

import java.util.Iterator;
import java.util.Set;

import net.minecraft.level.level.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.Level;

@SuppressWarnings("unchecked")
public class TileEntityHelper {

	public static void readFromPartialNBT(TileEntity te, CompoundTag source) {
		CompoundTag dest = new CompoundTag();
		te.write(dest);
		mergeNbt(source, dest);
		te.read(dest);
	}
	
	public static void readFromPartialNBT(Level world, int x, int y, int z, CompoundTag source) {
		if (source != null) {
			BlockPos pos = new BlockPos(x, y, z);
			TileEntity te = world.getTileEntity(pos);
			if (te == null) {
				te = tryToCreateGTPrefixBlockTileEntity(world.getBlockState(pos).getBlock());
				if (te != null) {
					world.setTileEntity(pos, te);
				}
			}
			if (te != null) {
				TileEntityHelper.readFromPartialNBT(te, source);
				BlockState state = world.getBlockState(pos);
				world.notifyBlockUpdate(pos, state, state, 2);
			}
		}
	}
	
	static private Class<? extends Block> prefixBlockClass;
	static private Class<? extends TileEntity> prefixBlockTileEntityClass;
	
	static {
		try {
			prefixBlockClass = (Class<? extends Block>) Class.forName("gregapi.block.prefixblock.PrefixBlock");
			prefixBlockTileEntityClass = (Class<? extends TileEntity>) Class.forName("gregapi.block.prefixblock.PrefixBlockTileEntity");
		} catch (ClassNotFoundException e) {
		}
	}
	
	public static CompoundTag tryToCreateGTPrefixBlockNBT(ItemStack ore) {
		Block block = ((BlockItem)ore.getItem()).getBlock();
		CompoundTag nbt = null;
		if (isGTPrefixBlock(block)) {
			nbt = new CompoundTag();
			nbt.putString("id", "gt.MetaBlockTileEntity");
		}
		return nbt;
	}
	
	private static boolean isGTPrefixBlock(Block block) {
		return prefixBlockClass != null && prefixBlockClass.isAssignableFrom(block.getClass());
	}
	
	private static TileEntity tryToCreateGTPrefixBlockTileEntity(Block block) {
		try {
			if (isGTPrefixBlock(block)) {
				return prefixBlockTileEntityClass.newInstance();
			}
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}

	private static void mergeNbt(CompoundTag source, CompoundTag dest) {
		Iterator<String> keys = ((Set<String>)source.keySet()).iterator(); 
		while(keys.hasNext()) {
			String key = keys.next();
			dest.put(key, source.getCompound(key));
		}
	}
	
}
