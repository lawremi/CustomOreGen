package CustomOreGen.Util;

import java.util.Iterator;
import java.util.Set;

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
			if (te != null) {
				TileEntityHelper.readFromPartialNBT(te, source);
			}
		}
	}
	
	private static void mergeNbt(NBTTagCompound source, NBTTagCompound dest) {
		Iterator<String> keys = ((Set<String>)source.func_150296_c()).iterator(); 
		while(keys.hasNext()) {
			String key = keys.next();
			dest.setTag(key, source.getTag(key));
		}
	}
	
}
