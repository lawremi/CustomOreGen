package ttftcuts.atg.api;

import java.util.Random;

import net.minecraft.world.World;

public interface IGenModCoords extends IGenMod {
	public int modify( int height, Random random, double rawHeight, int x, int z, World world );
}
