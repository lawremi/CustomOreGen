package CustomOreGen.Util;

import java.util.Random;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import CustomOreGen.Server.DistributionSettingMap.Copyable;
import CustomOreGen.Util.PDist.Type;

/* Conceptually, HeightScaledPDist could be an extension of PDist, or a decorator. 
 * It requires the world location to calculate some version of the height, or
 * at the very least it needs the height. This means that either 
 * (a) PDist.getValue needs to gain arguments
 * (b) HeightScaledPDist adds an overload or a new method getScaledValue and we expect it to be called instead
 * (c) HeightScaledPDist does NOT extend PDist but provides a similar API
 * 
 * (a) would add complexity but would not clarify what actually depends on height
 * (b) might make too easy to get unscaled values unintentionally
 * (c) not too bad delegating to field instead of super but loss of polymorphism is a tiny pain in parsing
 * 
 * For now, we have gone with (c), the decorator approach. No unifying interface, yet.
 */
public class HeightScaledPDist implements Copyable<HeightScaledPDist> {

	public PDist pdist;
	public HeightScale scaleFrom = new BaseHeightScale();
	public HeightScale scaleTo = new BaseHeightScale();

	public HeightScaledPDist(float mean, float range, Type type)
    {
        this.pdist = new PDist(mean, range, type);
    }

    public HeightScaledPDist(float mean, float range)
    {
        this(mean, range, Type.uniform);
    }

    public HeightScaledPDist()
    {
        this(0.0F, 0.0F, Type.uniform);
    }

	@Override
	public void copyFrom(HeightScaledPDist other) {
		this.pdist.copyFrom(other.pdist);
		this.scaleFrom = other.scaleFrom;
		this.scaleTo = other.scaleTo;
	}
	
	private float scale(float val, World world, int x, int z) {
		return val * scaleTo.getHeight(world, x, z) / scaleFrom.getHeight(world, x, z);
	}
	
	public float getValue(Random rand, World world, int x, int z) {
		return scale(pdist.getValue(rand), world, x, z); 
	}

	public float getValue(Random rand, World world, float x, float z) {
		return scale(pdist.getValue(rand), world, MathHelper.floor_float(x), MathHelper.floor_float(z)); 
	}
	
	public float getMax(World world, int x, int z) {
		return scale(pdist.getMax(), world, x, z);
	}

	public float getMin(World world, int x, int z) {
		return scale(pdist.getMin(), world, x, z);
	}
	
	public HeightScaledPDist set(float mean, float range, Type type, HeightScale scaleTo) {
		this.set(mean, range, type);
		this.scaleTo = scaleTo;
		return this;
	}

	public HeightScaledPDist set(float mean, float range, Type type) {
		pdist.set(mean, range, type);
		return this;
	}
	
	public int getIntValue(Random rand, World world, int x, int z)
    {
        float fval = this.getValue(rand, world, x, z);
        return PDist.roundToInt(fval, rand);
    }
	
	public String toString() {
		return pdist.toString() + " * " + scaleTo.getName() + "/" + scaleFrom.getName();
	}
	
}
