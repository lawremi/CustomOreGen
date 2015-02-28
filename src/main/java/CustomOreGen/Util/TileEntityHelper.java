package CustomOreGen.Util;

import java.util.Iterator;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class TileEntityHelper {

	public static void readFromPartialNBT(TileEntity te, NBTTagCompound source) {
		NBTTagCompound dest = new NBTTagCompound();
		te.writeToNBT(dest);
		mergeNbt(source, dest);
		te.readFromNBT(dest);
	}
	
	public static void readFromPartialNBT(World world, int x, int y, int z, NBTTagCompound source) {
		if (source != null) {
			TileEntity te = world.getTileEntity(x, y, z);
			if (te == null) {
				te = tryToCreateGTPrefixBlockTileEntity(world, x, y, z);
			}
			if (te != null) {
				TileEntityHelper.readFromPartialNBT(te, source);
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
	
	private static TileEntity tryToCreateGTPrefixBlockTileEntity(World world, int x, int y, int z) {
		if (prefixBlockClass != null) {
			try {
				Block block = world.getBlock(x, y, z);
				boolean isPrefixBlock = prefixBlockClass.isAssignableFrom(block.getClass());
				if (isPrefixBlock) {
					return prefixBlockTileEntityClass.newInstance();
				}
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			}
		}
		return null;
	}

	private static void mergeNbt(NBTTagCompound source, NBTTagCompound dest) {
		Iterator<String> keys = ((Set<String>)source.func_150296_c()).iterator(); 
		while(keys.hasNext()) {
			String key = keys.next();
			dest.setTag(key, source.getTag(key));
		}
	}
	
}
