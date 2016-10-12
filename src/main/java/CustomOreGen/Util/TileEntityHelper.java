package CustomOreGen.Util;

import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@SuppressWarnings("unchecked")
public class TileEntityHelper {

	public static void readFromPartialNBT(TileEntity te, NBTTagCompound source) {
		NBTTagCompound dest = new NBTTagCompound();
		te.writeToNBT(dest);
		mergeNbt(source, dest);
		te.readFromNBT(dest);
	}
	
	public static void readFromPartialNBT(World world, int x, int y, int z, NBTTagCompound source) {
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
				IBlockState state = world.getBlockState(pos);
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
	
	public static NBTTagCompound tryToCreateGTPrefixBlockNBT(ItemStack ore) {
		Block block = ((ItemBlock)ore.getItem()).block;
		NBTTagCompound nbt = null;
		if (isGTPrefixBlock(block)) {
			nbt = new NBTTagCompound();
			nbt.setShort("m", (short)ore.getItemDamage());
			nbt.setString("id", "gt.MetaBlockTileEntity");
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

	private static void mergeNbt(NBTTagCompound source, NBTTagCompound dest) {
		Iterator<String> keys = ((Set<String>)source.getKeySet()).iterator(); 
		while(keys.hasNext()) {
			String key = keys.next();
			dest.setTag(key, source.getTag(key));
		}
	}
	
}
