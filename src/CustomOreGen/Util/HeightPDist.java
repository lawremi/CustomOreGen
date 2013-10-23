package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/*
 * Thoughts about surface-relative height:
 * 
 * We want to generate distributions at a height relative to the surface at 
 * the chosen (X,Z). Also want to clamp by an absolute (bedrock-relative 
 * relative) height. <Substitute> already has minHeight, maxHeight settings, 
 * so we would need to call the new ones minAbsHeight and maxAbsHeight. That
 * avoids any ambiguity if we introduce a boolean setting "surfaceRelative".
 * This lacks cohesion though: "surfaceRelative" and the "Height" should be
 * part of the same setting. In more abstract terms, we need units, but
 * general unit support is outside the scope. Currently, we use dynamic 
 * expressions to achieve relativity to dimension height; dynamic 
 * expressions are not applicable at generation time, and changing that 
 * is outside the scope.
 * 
 * Currently, "Height" is a PDist, without support for other attributes. We
 * could create a HeightPDist that supports the relative mode. The problem
 * is that <Setting> always implies a PDist instance. This makes sense, but
 * it would be a bit annoying to have a <HeightSetting>. We might be able
 * to get away with this: 
 * <Setting name='Height' avg = '1' surfaceRelative='true'/>. 
 * 
 */

public class HeightPDist extends PDist {

	public boolean surfaceRelative;
	
	public HeightPDist(float mean, float range, Type type, boolean surfaceRelative) {
		super(mean, range, type);
		this.surfaceRelative = surfaceRelative;
	}
	
	public HeightPDist(float mean, float range, Type type) {
		super(mean, range, type);
	}

	public HeightPDist(float mean, float range) {
		super(mean, range);
	}

	public HeightPDist() {
		super();
	}

	public float getValue(Random rand, World world, float x, float z) {
		float y = this.getValue(rand);
		if (this.surfaceRelative) {
        	int iX = MathHelper.floor_float(x);
            int iZ = MathHelper.floor_float(z);
        	y += world.getHeightValue(iX, iZ);
        }
		return y;
	}
}
