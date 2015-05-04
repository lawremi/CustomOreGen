package CustomOreGen.Util;

import java.util.Random;

import CustomOreGen.Server.DistributionSettingMap.Copyable;

public class PDist implements Copyable<PDist>
{
    public float mean;
    public float range;
    public Type type;
        
    public enum Type
    {
        uniform,
        normal,
        inverse,
        inverseAbs;
    }

    public PDist(float mean, float range, Type type)
    {
        this.set(mean, range, type);
    }

    public PDist(float mean, float range)
    {
        this.set(mean, range, Type.uniform);
    }

    public PDist()
    {
        this.set(0.0F, 0.0F, Type.uniform);
    }

    public PDist(PDist pdist) {
		this.copyFrom(pdist);
	}

	public void copyFrom(PDist source)
    {
        this.mean = source.mean;
        this.range = source.range;
        this.type = source.type;
    }

    public PDist set(float mean, float range, Type type)
    {
        this.mean = mean;
        this.range = range >= 0.0F ? range : -range;
        this.type = type;
        return this;
    }

    public float getMax()
    {
        return this.mean + this.range;
    }

    public float getMin()
    {
        return this.mean - this.range;
    }

    public float getValue(Random rand)
    {
        if (this.range == 0.0F)
        {
            return this.mean;
        }
        else
        {
            switch (this.type)
            {
                case uniform:
                    return (rand.nextFloat() * 2.0F - 1.0F) * this.range + this.mean;

                case normal:
                    float value = (float)rand.nextGaussian() / 2.5F;

                    if (value < -1.0F)
                    {
                        value = -1.0F;
                    }
                    else if (value > 1.0F)
                    {
                        value = 1.0F;
                    }

                    return value * this.range + this.mean;
                case inverse:
                	value = inverseGaussian(1.5, 0.2F, rand)/2;
                	if (value > 1.0F)
                    {
                        value = 1.0F;
                    }
                	if(rand.nextBoolean()) value *= -1;
                    return value * this.range + this.mean;
                case inverseAbs:
                	value = inverseGaussian(1.5, 0.2F, rand)/2;
                	if (value > 1.0F)
                    {
                        value = 1.0F;
                    }
                    return value * this.range + this.mean;
                default:
                    return 0.0F;
            }
        }
    }

    private float inverseGaussian(double mu, double lambda, Random rand) {
    	/*Function taken from Wikipedia article on Inverse Gaussian Distribution*/
    	
    	double v = rand.nextGaussian();   // sample from a normal distribution with a mean of 0 and 1 standard deviation
    	v *= v;
    	double x = mu + (mu*mu*v)/(2*lambda) - (mu/(2*lambda)) * Math.sqrt(4*mu*lambda*v + mu*mu*v*v);
    	if (rand.nextDouble() <= (mu)/(mu + x))    // sample from a uniform distribution between 0 and 1
    		return (float)x;
    	else
    		return (float)((mu*mu)/x);
    }

    
    public int getIntValue(Random rand)
    {
        float fval = this.getValue(rand);
        return roundToInt(fval, rand);
    }

    public static int roundToInt(float fval, Random rand) {
    	int ival = (int)fval;
        fval -= (float)ival;

        if (fval > 0.0F && fval > rand.nextFloat())
        {
            ++ival;
        }
        else if (fval < 0.0F && -fval > rand.nextFloat())
        {
            --ival;
        }

        return ival;
	}

    public float standardize(float x) {
    	return (x - this.mean) / this.range;
    }
    
    public float cdf(float x) {
    	switch (this.type)
        {
            case uniform:
                return standardize(x);

            case normal:
            	float z = standardize(x);
                return (float)(1/(1 + Math.exp(-0.07056 * Math.pow(z, 3) - 1.5976 * z)));
                
            case inverse:
            case inverseAbs:
			float m = (float)Math.sqrt(1/x);
            	//taken from Wolfram Alpha "graph cumulative wald distribution mean 1 scale 0.2"
            	return 0.5f*erfc(0.316228f*(1-x)*m) + 0.745912f*erfc(0.316228f*(1+x)*m);
            	
            default:
                return 0.0F;
        }
    }
    
    /*
     * complementary error function, defined as 1-erf(x)
     * erf(x) is defined as pi^(-0.5) * gamma(0.5, x^2)  
     */
    private float erfc(float x) {
    	return 1 - (0.56418f*(float)GammaFunction.incompleteGammaP(0.5,x*x));
    }
    
	public String toString()
    {
        return String.format("%f +- %f %s", new Object[] {Float.valueOf(this.mean), Float.valueOf(this.range), this.type.name()});
    }
}
