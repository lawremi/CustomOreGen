package CustomOreGen.Util;

import java.util.Random;

public class NoiseGenerator
{
    private static Grad[] grad2 = new Grad[] {new Grad(1.0D, 0.0D, 0.0D), new Grad(-1.0D, 0.0D, 0.0D), new Grad(0.0D, 1.0D, 0.0D), new Grad(0.0D, -1.0D, 0.0D)};
    private static Grad[] grad3 = new Grad[] {new Grad(1.0D, 1.0D, 0.0D), new Grad(-1.0D, 1.0D, 0.0D), new Grad(1.0D, -1.0D, 0.0D), new Grad(-1.0D, -1.0D, 0.0D), new Grad(1.0D, 0.0D, 1.0D), new Grad(-1.0D, 0.0D, 1.0D), new Grad(1.0D, 0.0D, -1.0D), new Grad(-1.0D, 0.0D, -1.0D), new Grad(0.0D, 1.0D, 1.0D), new Grad(0.0D, -1.0D, 1.0D), new Grad(0.0D, 1.0D, -1.0D), new Grad(0.0D, -1.0D, -1.0D)};
    private static Grad[] grad4 = new Grad[] {new Grad(0.0D, 1.0D, 1.0D, 1.0D), new Grad(0.0D, 1.0D, 1.0D, -1.0D), new Grad(0.0D, 1.0D, -1.0D, 1.0D), new Grad(0.0D, 1.0D, -1.0D, -1.0D), new Grad(0.0D, -1.0D, 1.0D, 1.0D), new Grad(0.0D, -1.0D, 1.0D, -1.0D), new Grad(0.0D, -1.0D, -1.0D, 1.0D), new Grad(0.0D, -1.0D, -1.0D, -1.0D), new Grad(1.0D, 0.0D, 1.0D, 1.0D), new Grad(1.0D, 0.0D, 1.0D, -1.0D), new Grad(1.0D, 0.0D, -1.0D, 1.0D), new Grad(1.0D, 0.0D, -1.0D, -1.0D), new Grad(-1.0D, 0.0D, 1.0D, 1.0D), new Grad(-1.0D, 0.0D, 1.0D, -1.0D), new Grad(-1.0D, 0.0D, -1.0D, 1.0D), new Grad(-1.0D, 0.0D, -1.0D, -1.0D), new Grad(1.0D, 1.0D, 0.0D, 1.0D), new Grad(1.0D, 1.0D, 0.0D, -1.0D), new Grad(1.0D, -1.0D, 0.0D, 1.0D), new Grad(1.0D, -1.0D, 0.0D, -1.0D), new Grad(-1.0D, 1.0D, 0.0D, 1.0D), new Grad(-1.0D, 1.0D, 0.0D, -1.0D), new Grad(-1.0D, -1.0D, 0.0D, 1.0D), new Grad(-1.0D, -1.0D, 0.0D, -1.0D), new Grad(1.0D, 1.0D, 1.0D, 0.0D), new Grad(1.0D, 1.0D, -1.0D, 0.0D), new Grad(1.0D, -1.0D, 1.0D, 0.0D), new Grad(1.0D, -1.0D, -1.0D, 0.0D), new Grad(-1.0D, 1.0D, 1.0D, 0.0D), new Grad(-1.0D, 1.0D, -1.0D, 0.0D), new Grad(-1.0D, -1.0D, 1.0D, 0.0D), new Grad(-1.0D, -1.0D, -1.0D, 0.0D)};
    private final byte[] permutation = new byte[] {(byte)17, (byte)1, (byte)31, (byte)92, (byte)22, (byte)53, (byte)127, (byte)38, (byte)84, (byte)62, (byte)54, (byte)21, (byte)123, (byte)111, (byte)49, (byte)96, (byte)95, (byte)58, (byte)104, (byte)42, (byte)34, (byte)55, (byte)78, (byte)107, (byte)105, (byte)98, (byte)39, (byte)50, (byte)125, (byte)2, (byte)91, (byte)23, (byte)119, (byte)100, (byte)70, (byte)56, (byte)3, (byte)88, (byte)66, (byte)101, (byte)29, (byte)8, (byte)43, (byte)76, (byte)124, (byte)0, (byte)15, (byte)115, (byte)83, (byte)5, (byte)19, (byte)64, (byte)106, (byte)80, (byte)48, (byte)82, (byte)121, (byte)69, (byte)117, (byte)10, (byte)61, (byte)9, (byte)109, (byte)87, (byte)94, (byte)25, (byte)20, (byte)27, (byte)102, (byte)122, (byte)60, (byte)6, (byte)33, (byte)77, (byte)89, (byte)90, (byte)118, (byte)110, (byte)75, (byte)71, (byte)113, (byte)126, (byte)67, (byte)52, (byte)74, (byte)116, (byte)44, (byte)103, (byte)14, (byte)18, (byte)93, (byte)85, (byte)108, (byte)12, (byte)32, (byte)45, (byte)47, (byte)79, (byte)68, (byte)86, (byte)24, (byte)114, (byte)30, (byte)57, (byte)59, (byte)81, (byte)4, (byte)26, (byte)36, (byte)37, (byte)120, (byte)35, (byte)65, (byte)28, (byte)7, (byte)63, (byte)40, (byte)72, (byte)73, (byte)41, (byte)99, (byte)16, (byte)97, (byte)13, (byte)112, (byte)51, (byte)11, (byte)46};
    private static final double F2 = 0.5D * (Math.sqrt(3.0D) - 1.0D);
    private static final double G2 = (3.0D - Math.sqrt(3.0D)) / 6.0D;
    private static final double F3 = 0.3333333333333333D;
    private static final double G3 = 0.16666666666666666D;
    private static final double F4 = (Math.sqrt(5.0D) - 1.0D) / 4.0D;
    private static final double G4 = (5.0D - Math.sqrt(5.0D)) / 20.0D;

    public NoiseGenerator() {}

    public NoiseGenerator(Random random)
    {
        if (random != null)
        {
            for (int i = 0; i < this.permutation.length; ++i)
            {
                int rnd = random.nextInt(this.permutation.length);
                byte swap = this.permutation[rnd];
                this.permutation[rnd] = this.permutation[i];
                this.permutation[i] = swap;
            }
        }
    }

    public double noise(double xin, double yin)
    {
        double s = (xin + yin) * F2;
        int i = fastfloor(xin + s);
        int j = fastfloor(yin + s);
        double t = (double)(i + j) * G2;
        double X0 = (double)i - t;
        double Y0 = (double)j - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;
        byte i1;
        byte j1;

        if (x0 > y0)
        {
            i1 = 1;
            j1 = 0;
        }
        else
        {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - (double)i1 + G2;
        double y1 = y0 - (double)j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & 255;
        int jj = j & 255;
        Grad gi0 = this.getGradient(ii, jj);
        Grad gi1 = this.getGradient(ii + i1, jj + j1);
        Grad gi2 = this.getGradient(ii + 1, jj + 1);
        double t0 = 0.5D - x0 * x0 - y0 * y0;
        double n0;

        if (t0 < 0.0D)
        {
            n0 = 0.0D;
        }
        else
        {
            t0 *= t0;
            n0 = t0 * t0 * dot(gi0, x0, y0);
        }

        double t1 = 0.5D - x1 * x1 - y1 * y1;
        double n1;

        if (t1 < 0.0D)
        {
            n1 = 0.0D;
        }
        else
        {
            t1 *= t1;
            n1 = t1 * t1 * dot(gi1, x1, y1);
        }

        double t2 = 0.5D - x2 * x2 - y2 * y2;
        double n2;

        if (t2 < 0.0D)
        {
            n2 = 0.0D;
        }
        else
        {
            t2 *= t2;
            n2 = t2 * t2 * dot(gi2, x2, y2);
        }

        return 70.0D * (n0 + n1 + n2);
    }

    public double noise(double xin, double yin, double zin)
    {
        double s = (xin + yin + zin) * 0.3333333333333333D;
        int i = fastfloor(xin + s);
        int j = fastfloor(yin + s);
        int k = fastfloor(zin + s);
        double t = (double)(i + j + k) * 0.16666666666666666D;
        double X0 = (double)i - t;
        double Y0 = (double)j - t;
        double Z0 = (double)k - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;
        double z0 = zin - Z0;
        byte i1;
        byte j1;
        byte j2;
        byte k2;
        byte k1;
        byte i2;

        if (x0 >= y0)
        {
            if (y0 >= z0)
            {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            }
            else if (x0 >= z0)
            {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
            else
            {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        }
        else if (y0 < z0)
        {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        }
        else if (x0 < z0)
        {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        }
        else
        {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
        }

        double x1 = x0 - (double)i1 + 0.16666666666666666D;
        double y1 = y0 - (double)j1 + 0.16666666666666666D;
        double z1 = z0 - (double)k1 + 0.16666666666666666D;
        double x2 = x0 - (double)i2 + 0.3333333333333333D;
        double y2 = y0 - (double)j2 + 0.3333333333333333D;
        double z2 = z0 - (double)k2 + 0.3333333333333333D;
        double x3 = x0 - 1.0D + 0.5D;
        double y3 = y0 - 1.0D + 0.5D;
        double z3 = z0 - 1.0D + 0.5D;
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        Grad gi0 = this.getGradient(ii, jj, kk);
        Grad gi1 = this.getGradient(ii + i1, jj + j1, kk + k1);
        Grad gi2 = this.getGradient(ii + i2, jj + j2, kk + k2);
        Grad gi3 = this.getGradient(ii + 1, jj + 1, kk + 1);
        double t0 = 0.6D - x0 * x0 - y0 * y0 - z0 * z0;
        double n0;

        if (t0 < 0.0D)
        {
            n0 = 0.0D;
        }
        else
        {
            t0 *= t0;
            n0 = t0 * t0 * dot(gi0, x0, y0, z0);
        }

        double t1 = 0.6D - x1 * x1 - y1 * y1 - z1 * z1;
        double n1;

        if (t1 < 0.0D)
        {
            n1 = 0.0D;
        }
        else
        {
            t1 *= t1;
            n1 = t1 * t1 * dot(gi1, x1, y1, z1);
        }

        double t2 = 0.6D - x2 * x2 - y2 * y2 - z2 * z2;
        double n2;

        if (t2 < 0.0D)
        {
            n2 = 0.0D;
        }
        else
        {
            t2 *= t2;
            n2 = t2 * t2 * dot(gi2, x2, y2, z2);
        }

        double t3 = 0.6D - x3 * x3 - y3 * y3 - z3 * z3;
        double n3;

        if (t3 < 0.0D)
        {
            n3 = 0.0D;
        }
        else
        {
            t3 *= t3;
            n3 = t3 * t3 * dot(gi3, x3, y3, z3);
        }

        return 32.0D * (n0 + n1 + n2 + n3);
    }

    public double noise(double x, double y, double z, double w)
    {
        double s = (x + y + z + w) * F4;
        int i = fastfloor(x + s);
        int j = fastfloor(y + s);
        int k = fastfloor(z + s);
        int l = fastfloor(w + s);
        double t = (double)(i + j + k + l) * G4;
        double X0 = (double)i - t;
        double Y0 = (double)j - t;
        double Z0 = (double)k - t;
        double W0 = (double)l - t;
        double x0 = x - X0;
        double y0 = y - Y0;
        double z0 = z - Z0;
        double w0 = w - W0;
        int rankx = 0;
        int ranky = 0;
        int rankz = 0;
        int rankw = 0;

        if (x0 > y0)
        {
            ++rankx;
        }
        else
        {
            ++ranky;
        }

        if (x0 > z0)
        {
            ++rankx;
        }
        else
        {
            ++rankz;
        }

        if (x0 > w0)
        {
            ++rankx;
        }
        else
        {
            ++rankw;
        }

        if (y0 > z0)
        {
            ++ranky;
        }
        else
        {
            ++rankz;
        }

        if (y0 > w0)
        {
            ++ranky;
        }
        else
        {
            ++rankw;
        }

        if (z0 > w0)
        {
            ++rankz;
        }
        else
        {
            ++rankw;
        }

        int i1 = rankx >= 3 ? 1 : 0;
        int j1 = ranky >= 3 ? 1 : 0;
        int k1 = rankz >= 3 ? 1 : 0;
        int l1 = rankw >= 3 ? 1 : 0;
        int i2 = rankx >= 2 ? 1 : 0;
        int j2 = ranky >= 2 ? 1 : 0;
        int k2 = rankz >= 2 ? 1 : 0;
        int l2 = rankw >= 2 ? 1 : 0;
        int i3 = rankx >= 1 ? 1 : 0;
        int j3 = ranky >= 1 ? 1 : 0;
        int k3 = rankz >= 1 ? 1 : 0;
        int l3 = rankw >= 1 ? 1 : 0;
        double x1 = x0 - (double)i1 + G4;
        double y1 = y0 - (double)j1 + G4;
        double z1 = z0 - (double)k1 + G4;
        double w1 = w0 - (double)l1 + G4;
        double x2 = x0 - (double)i2 + 2.0D * G4;
        double y2 = y0 - (double)j2 + 2.0D * G4;
        double z2 = z0 - (double)k2 + 2.0D * G4;
        double w2 = w0 - (double)l2 + 2.0D * G4;
        double x3 = x0 - (double)i3 + 3.0D * G4;
        double y3 = y0 - (double)j3 + 3.0D * G4;
        double z3 = z0 - (double)k3 + 3.0D * G4;
        double w3 = w0 - (double)l3 + 3.0D * G4;
        double x4 = x0 - 1.0D + 4.0D * G4;
        double y4 = y0 - 1.0D + 4.0D * G4;
        double z4 = z0 - 1.0D + 4.0D * G4;
        double w4 = w0 - 1.0D + 4.0D * G4;
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int ll = l & 255;
        Grad gi0 = this.getGradient(ii, jj, kk, ll);
        Grad gi1 = this.getGradient(ii + i1, jj + j1, kk + k1, ll + l1);
        Grad gi2 = this.getGradient(ii + i2, jj + j2, kk + k2, ll + l2);
        Grad gi3 = this.getGradient(ii + i3, jj + j3, kk + k3, ll + l3);
        Grad gi4 = this.getGradient(ii + 1, jj + 1, kk + 1, ll + 1);
        double t0 = 0.6D - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
        double n0;

        if (t0 < 0.0D)
        {
            n0 = 0.0D;
        }
        else
        {
            t0 *= t0;
            n0 = t0 * t0 * dot(gi0, x0, y0, z0, w0);
        }

        double t1 = 0.6D - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
        double n1;

        if (t1 < 0.0D)
        {
            n1 = 0.0D;
        }
        else
        {
            t1 *= t1;
            n1 = t1 * t1 * dot(gi1, x1, y1, z1, w1);
        }

        double t2 = 0.6D - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
        double n2;

        if (t2 < 0.0D)
        {
            n2 = 0.0D;
        }
        else
        {
            t2 *= t2;
            n2 = t2 * t2 * dot(gi2, x2, y2, z2, w2);
        }

        double t3 = 0.6D - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3;
        double n3;

        if (t3 < 0.0D)
        {
            n3 = 0.0D;
        }
        else
        {
            t3 *= t3;
            n3 = t3 * t3 * dot(gi3, x3, y3, z3, w3);
        }

        double t4 = 0.6D - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4;
        double n4;

        if (t4 < 0.0D)
        {
            n4 = 0.0D;
        }
        else
        {
            t4 *= t4;
            n4 = t4 * t4 * dot(gi4, x4, y4, z4, w4);
        }

        return 27.0D * (n0 + n1 + n2 + n3 + n4);
    }

    private static int fastfloor(double x)
    {
        int xi = (int)x;
        return x < (double)xi ? xi - 1 : xi;
    }

    private static double dot(Grad g, double x, double y)
    {
        return g.x * x + g.y * y;
    }

    private static double dot(Grad g, double x, double y, double z)
    {
        return g.x * x + g.y * y + g.z * z;
    }

    private static double dot(Grad g, double x, double y, double z, double w)
    {
        return g.x * x + g.y * y + g.z * z + g.w * w;
    }

    private final Grad getGradient(int x, int y)
    {
        int xm = x + this.permutation[y & 127];
        byte pm = this.permutation[xm & 127];
        return grad2[pm & 3];
    }

    private final Grad getGradient(int x, int y, int z)
    {
        int ym = y + this.permutation[z & 127];
        int xm = x + this.permutation[ym & 127];
        byte pm = this.permutation[xm & 127];
        return grad3[pm % 12];
    }

    private final Grad getGradient(int x, int y, int z, int w)
    {
        int zm = z + this.permutation[w & 127];
        int ym = y + this.permutation[zm & 127];
        int xm = x + this.permutation[ym & 127];
        byte pm = this.permutation[xm & 127];
        return grad4[pm & 49];
    }
    
    private static class Grad
    {
        double x;
        double y;
        double z;
        double w;

        Grad(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Grad(double x, double y, double z, double w)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

}
