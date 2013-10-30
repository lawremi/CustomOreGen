package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/*
 * Thoughts about surface-relative height:
 * 
 * We want to generate distributions at a height relative to the surface at 
 * the chosen (X,Z). This is infeasible right now for MapGen-based 
 * distributions, because they generate prior to chunk generation. 
 * It would require a significant amount of work to get around this. 
 * 
 */

public class HeightPDist extends PDist {

	public HeightPDist(float mean, float range, Type type) {
		super(mean, range, type);
	}

	public HeightPDist(float mean, float range) {
		super(mean, range);
	}

	public HeightPDist() {
		super();
	}

	public float getValue(Random rand, World world) {
		float y = this.getValue(rand);
		boolean fractional = this.mean > 0 && this.mean < 1;
		if (fractional) {
        	y *= world.provider.getAverageGroundLevel();
        }
		return y;
	}
	
	public void copyFrom(PDist source)
    {
        this.mean = source.mean;
        this.range = source.range;
        this.type = source.type;
    }
}
