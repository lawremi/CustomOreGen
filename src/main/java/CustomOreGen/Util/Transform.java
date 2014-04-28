package CustomOreGen.Util;

public class Transform implements Cloneable
{
    private float[] mat;

    public Transform()
    {
        this.mat = new float[] {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    }

    protected Transform(float[] matrix)
    {
        this.mat = matrix;
    }

    public Transform clone()
    {
        return new Transform((float[])this.mat.clone());
    }

    public float element(int row, int col)
    {
        return this.mat[(row & 3) << 2 | col & 3];
    }

    public void setElement(int row, int col, float value)
    {
        this.mat[(row & 3) << 2 | col & 3] = value;
    }

    public float[] elements()
    {
        return this.mat;
    }

    public Transform identity()
    {
        this.mat[0] = 1.0F;
        this.mat[1] = 0.0F;
        this.mat[2] = 0.0F;
        this.mat[3] = 0.0F;
        this.mat[4] = 0.0F;
        this.mat[5] = 1.0F;
        this.mat[6] = 0.0F;
        this.mat[7] = 0.0F;
        this.mat[8] = 0.0F;
        this.mat[9] = 0.0F;
        this.mat[10] = 1.0F;
        this.mat[11] = 0.0F;
        this.mat[12] = 0.0F;
        this.mat[13] = 0.0F;
        this.mat[14] = 0.0F;
        this.mat[15] = 1.0F;
        return this;
    }

    public Transform transform(Transform trans)
    {
        mult(this.mat, trans.mat);
        return this;
    }

    public void transformVector(float[] vector)
    {
        float vw = vector.length > 3 ? vector[3] : 1.0F;
        float x = this.mat[0] * vector[0] + this.mat[1] * vector[1] + this.mat[2] * vector[2] + this.mat[3] * vw;
        float y = this.mat[4] * vector[0] + this.mat[5] * vector[1] + this.mat[6] * vector[2] + this.mat[7] * vw;
        float z = this.mat[8] * vector[0] + this.mat[9] * vector[1] + this.mat[10] * vector[2] + this.mat[11] * vw;
        float w = this.mat[12] * vector[0] + this.mat[13] * vector[1] + this.mat[14] * vector[2] + this.mat[15] * vw;
        vector[0] = x;
        vector[1] = y;
        vector[2] = z;

        if (vector.length > 3)
        {
            vector[3] = w;
        }
    }

    public void transformVectors(float[] vectors, int size, int base, int count)
    {
        if (size >= 1 && size <= 4)
        {
            if (vectors.length < base + count * size)
            {
                throw new RuntimeException("Attempting to transform vector array that is too short.");
            }
            else
            {
                for (int offset = base; offset < base + count * size; offset += size)
                {
                    float vx = vectors[offset + 0];
                    float vy = size > 1 ? vectors[offset + 1] : 0.0F;
                    float vz = size > 2 ? vectors[offset + 2] : 0.0F;
                    float vw = size > 3 ? vectors[offset + 3] : 1.0F;
                    vectors[offset + 0] = this.mat[0] * vx + this.mat[1] * vy + this.mat[2] * vz + this.mat[3] * vw;

                    if (size > 1)
                    {
                        vectors[offset + 1] = this.mat[4] * vx + this.mat[5] * vy + this.mat[6] * vz + this.mat[7] * vw;
                    }

                    if (size > 2)
                    {
                        vectors[offset + 2] = this.mat[8] * vx + this.mat[9] * vy + this.mat[10] * vz + this.mat[11] * vw;
                    }

                    if (size > 3)
                    {
                        vectors[offset + 3] = this.mat[12] * vx + this.mat[13] * vy + this.mat[14] * vz + this.mat[15] * vw;
                    }
                }
            }
        }
        else
        {
            throw new RuntimeException("Attempting to transform vectors of invalid size.");
        }
    }

    public void transformBB(float[] bounds)
    {
        float[] v = new float[3];
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int c = 0; c < 8; ++c)
        {
            v[0] = bounds[(c & 1) == 0 ? 0 : 3];
            v[1] = bounds[(c & 2) == 0 ? 1 : 4];
            v[2] = bounds[(c & 4) == 0 ? 2 : 5];
            this.transformVector(v);

            if (v[0] < minX)
            {
                minX = v[0];
            }

            if (v[1] < minY)
            {
                minY = v[1];
            }

            if (v[2] < minZ)
            {
                minZ = v[2];
            }

            if (v[0] > maxX)
            {
                maxX = v[0];
            }

            if (v[1] > maxY)
            {
                maxY = v[1];
            }

            if (v[2] > maxZ)
            {
                maxZ = v[2];
            }
        }

        bounds[0] = minX;
        bounds[1] = minY;
        bounds[2] = minZ;
        bounds[3] = maxX;
        bounds[4] = maxY;
        bounds[5] = maxZ;
    }

    public Transform rotate(float angle, float axisX, float axisY, float axisZ)
    {
        float r = axisX * axisX + axisY * axisY + axisZ * axisZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to rotate about a null vector");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                axisX /= r;
                axisY /= r;
                axisZ /= r;
            }

            float s = (float)Math.sin((double)angle);
            float nc = 1.0F - (float)Math.cos((double)angle);
            float[] rot = new float[16];
            rot[0] = 1.0F + (axisX * axisX - 1.0F) * nc;
            rot[1] = axisX * axisY * nc - axisZ * s;
            rot[2] = axisX * axisZ * nc + axisY * s;
            rot[4] = axisX * axisY * nc + axisZ * s;
            rot[5] = 1.0F + (axisY * axisY - 1.0F) * nc;
            rot[6] = axisY * axisZ * nc - axisX * s;
            rot[8] = axisX * axisZ * nc - axisY * s;
            rot[9] = axisY * axisZ * nc + axisX * s;
            rot[10] = 1.0F + (axisZ * axisZ - 1.0F) * nc;
            rot[15] = 1.0F;
            mult(this.mat, rot);
            return this;
        }
    }

    public Transform rotateX(float angle)
    {
        float s = (float)Math.sin((double)angle);
        float c = (float)Math.cos((double)angle);
        float tmp = 0.0F;
        tmp = this.mat[1];
        this.mat[1] = tmp * c + this.mat[2] * s;
        this.mat[2] = this.mat[2] * c - tmp * s;
        tmp = this.mat[5];
        this.mat[5] = tmp * c + this.mat[6] * s;
        this.mat[6] = this.mat[6] * c - tmp * s;
        tmp = this.mat[9];
        this.mat[9] = tmp * c + this.mat[10] * s;
        this.mat[10] = this.mat[10] * c - tmp * s;
        tmp = this.mat[13];
        this.mat[13] = tmp * c + this.mat[14] * s;
        this.mat[14] = this.mat[14] * c - tmp * s;
        return this;
    }

    public Transform rotateY(float angle)
    {
        float s = (float)Math.sin((double)angle);
        float c = (float)Math.cos((double)angle);
        float tmp = 0.0F;
        tmp = this.mat[0];
        this.mat[0] = tmp * c - this.mat[2] * s;
        this.mat[2] = this.mat[2] * c + tmp * s;
        tmp = this.mat[4];
        this.mat[4] = tmp * c - this.mat[6] * s;
        this.mat[6] = this.mat[6] * c + tmp * s;
        tmp = this.mat[8];
        this.mat[8] = tmp * c - this.mat[10] * s;
        this.mat[10] = this.mat[10] * c + tmp * s;
        tmp = this.mat[12];
        this.mat[12] = tmp * c - this.mat[14] * s;
        this.mat[14] = this.mat[14] * c + tmp * s;
        return this;
    }

    public Transform rotateZ(float angle)
    {
        float s = (float)Math.sin((double)angle);
        float c = (float)Math.cos((double)angle);
        float tmp = 0.0F;
        tmp = this.mat[0];
        this.mat[0] = tmp * c + this.mat[1] * s;
        this.mat[1] = this.mat[1] * c - tmp * s;
        tmp = this.mat[4];
        this.mat[4] = tmp * c + this.mat[5] * s;
        this.mat[5] = this.mat[5] * c - tmp * s;
        tmp = this.mat[8];
        this.mat[8] = tmp * c + this.mat[9] * s;
        this.mat[9] = this.mat[9] * c - tmp * s;
        tmp = this.mat[12];
        this.mat[12] = tmp * c + this.mat[13] * s;
        this.mat[13] = this.mat[13] * c - tmp * s;
        return this;
    }

    public Transform rotateXInto(float axisX, float axisY, float axisZ)
    {
        float r = axisX * axisX + axisY * axisY + axisZ * axisZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to rotate into a null vector");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                axisX /= r;
                axisY /= r;
                axisZ /= r;
            }

            float s2 = axisY * axisY + axisZ * axisZ;

            if (s2 == 0.0F)
            {
                return this;
            }
            else
            {
                float[] rot = new float[16];
                rot[0] = axisX;
                rot[1] = -axisY;
                rot[2] = -axisZ;
                rot[4] = axisY;
                rot[5] = (axisY * axisY * axisX + axisZ * axisZ) / s2;
                rot[6] = axisY * axisZ * (axisX - 1.0F) / s2;
                rot[8] = axisZ;
                rot[9] = axisY * axisZ * (axisX - 1.0F) / s2;
                rot[10] = (axisY * axisY + axisZ * axisZ * axisX) / s2;
                rot[15] = 1.0F;
                mult(this.mat, rot);
                return this;
            }
        }
    }

    public Transform rotateYInto(float axisX, float axisY, float axisZ)
    {
        float r = axisX * axisX + axisY * axisY + axisZ * axisZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to rotate into a null vector");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                axisX /= r;
                axisY /= r;
                axisZ /= r;
            }

            float s2 = axisX * axisX + axisZ * axisZ;

            if (s2 == 0.0F)
            {
                return this;
            }
            else
            {
                float[] rot = new float[16];
                rot[0] = (axisX * axisX * axisY + axisZ * axisZ) / s2;
                rot[1] = axisX;
                rot[2] = axisX * axisZ * (axisY - 1.0F) / s2;
                rot[4] = -axisX;
                rot[5] = axisY;
                rot[6] = -axisZ;
                rot[8] = axisX * axisZ * (axisY - 1.0F) / s2;
                rot[9] = axisZ;
                rot[10] = (axisX * axisX + axisZ * axisZ * axisY) / s2;
                rot[15] = 1.0F;
                mult(this.mat, rot);
                return this;
            }
        }
    }

    public Transform rotateZInto(float axisX, float axisY, float axisZ)
    {
        float r = axisX * axisX + axisY * axisY + axisZ * axisZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to rotate into a null vector");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                axisX /= r;
                axisY /= r;
                axisZ /= r;
            }

            float s2 = axisX * axisX + axisY * axisY;

            if (s2 == 0.0F)
            {
                return this;
            }
            else
            {
                float[] rot = new float[16];
                rot[0] = (axisX * axisX * axisZ + axisY * axisY) / s2;
                rot[1] = axisX * axisY * (axisZ - 1.0F) / s2;
                rot[2] = axisX;
                rot[4] = axisX * axisY * (axisZ - 1.0F) / s2;
                rot[5] = (axisX * axisX + axisY * axisY * axisZ) / s2;
                rot[6] = axisY;
                rot[8] = -axisX;
                rot[9] = -axisY;
                rot[10] = axisZ;
                rot[15] = 1.0F;
                mult(this.mat, rot);
                return this;
            }
        }
    }

    public Transform scale(float scaleM, float axisX, float axisY, float axisZ)
    {
        float r = axisX * axisX + axisY * axisY + axisZ * axisZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to scale along a null vector");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                axisX /= r;
                axisY /= r;
                axisZ /= r;
            }

            --scaleM;
            float[] scal = new float[16];
            scal[0] = scaleM * axisX * axisX + 1.0F;
            scal[1] = scaleM * axisX * axisY;
            scal[2] = scaleM * axisX * axisZ;
            scal[4] = scaleM * axisX * axisY;
            scal[5] = scaleM * axisY * axisY + 1.0F;
            scal[6] = scaleM * axisY * axisZ;
            scal[8] = scaleM * axisX * axisZ;
            scal[9] = scaleM * axisY * axisZ;
            scal[10] = scaleM * axisZ * axisZ + 1.0F;
            scal[15] = 1.0F;
            mult(this.mat, scal);
            return this;
        }
    }

    public Transform scale(float scaleX, float scaleY, float scaleZ)
    {
        this.mat[0] *= scaleX;
        this.mat[1] *= scaleY;
        this.mat[2] *= scaleZ;
        this.mat[4] *= scaleX;
        this.mat[5] *= scaleY;
        this.mat[6] *= scaleZ;
        this.mat[8] *= scaleX;
        this.mat[9] *= scaleY;
        this.mat[10] *= scaleZ;
        this.mat[12] *= scaleX;
        this.mat[13] *= scaleY;
        this.mat[14] *= scaleZ;
        return this;
    }

    public Transform shear(float angle, float shearX, float shearY, float shearZ, float invariantX, float invariantY, float invariantZ)
    {
        float ri = invariantX * invariantX + invariantY * invariantY + invariantZ * invariantZ;

        if (ri == 0.0F)
        {
            throw new RuntimeException("Attempting to shear with a null invariant vector");
        }
        else
        {
            if (ri != 1.0F)
            {
                ri = (float)Math.sqrt((double)ri);
                invariantX /= ri;
                invariantY /= ri;
                invariantZ /= ri;
            }

            float p = shearX * invariantX + shearY * invariantY + shearZ * invariantZ;

            if (p != 0.0F)
            {
                shearX -= p * invariantX;
                shearY -= p * invariantY;
                shearZ -= p * invariantZ;
            }

            float rs = shearX * shearX + shearY * shearY + shearZ * shearZ;

            if (rs == 0.0F)
            {
                throw new RuntimeException("Attempting to shear with a null or parallel shear vector");
            }
            else
            {
                if (rs != 1.0F)
                {
                    rs = (float)Math.sqrt((double)rs);
                    shearX /= rs;
                    shearY /= rs;
                    shearZ /= rs;
                }

                float t = (float)Math.tan((double)angle);
                float[] shr = new float[16];
                shr[0] = shearX * invariantX * t + 1.0F;
                shr[1] = shearX * invariantY * t;
                shr[2] = shearX * invariantZ * t;
                shr[4] = shearY * invariantX * t;
                shr[5] = shearY * invariantY * t + 1.0F;
                shr[6] = shearY * invariantZ * t;
                shr[8] = shearZ * invariantX * t;
                shr[9] = shearZ * invariantY * t;
                shr[10] = shearZ * invariantZ * t + 1.0F;
                shr[15] = 1.0F;
                mult(this.mat, shr);
                return this;
            }
        }
    }

    public Transform reflect(float mirrorNormalX, float mirrorNormalY, float mirrorNormalZ)
    {
        float r = mirrorNormalX * mirrorNormalX + mirrorNormalY * mirrorNormalY + mirrorNormalZ * mirrorNormalZ;

        if (r == 0.0F)
        {
            throw new RuntimeException("Attempting to reflect across a null plane");
        }
        else
        {
            if (r != 1.0F)
            {
                r = (float)Math.sqrt((double)r);
                mirrorNormalX /= r;
                mirrorNormalY /= r;
                mirrorNormalZ /= r;
            }

            float[] refl = new float[16];
            refl[0] = 1.0F - 2.0F * mirrorNormalX * mirrorNormalX;
            refl[1] = -2.0F * mirrorNormalX * mirrorNormalY;
            refl[2] = -2.0F * mirrorNormalX * mirrorNormalZ;
            refl[4] = -2.0F * mirrorNormalX * mirrorNormalY;
            refl[5] = 1.0F - 2.0F * mirrorNormalY * mirrorNormalY;
            refl[6] = -2.0F * mirrorNormalY * mirrorNormalZ;
            refl[8] = -2.0F * mirrorNormalX * mirrorNormalZ;
            refl[9] = -2.0F * mirrorNormalY * mirrorNormalZ;
            refl[10] = 1.0F - 2.0F * mirrorNormalZ * mirrorNormalZ;
            refl[15] = 1.0F;
            mult(this.mat, refl);
            return this;
        }
    }

    public Transform translate(float transX, float transY, float transZ)
    {
        this.mat[3] += this.mat[0] * transX + this.mat[1] * transY + this.mat[2] * transZ;
        this.mat[7] += this.mat[4] * transX + this.mat[5] * transY + this.mat[6] * transZ;
        this.mat[11] += this.mat[8] * transX + this.mat[9] * transY + this.mat[10] * transZ;
        this.mat[15] += this.mat[12] * transX + this.mat[13] * transY + this.mat[14] * transZ;
        return this;
    }

    public float determinant()
    {
        return this.mat[0] * (this.mat[5] * (this.mat[10] * this.mat[15] - this.mat[11] * this.mat[14]) + this.mat[6] * (this.mat[11] * this.mat[13] - this.mat[15] * this.mat[9]) + this.mat[7] * (this.mat[14] * this.mat[9] - this.mat[10] * this.mat[13])) + this.mat[1] * (this.mat[4] * (this.mat[11] * this.mat[14] - this.mat[10] * this.mat[15]) + this.mat[6] * (this.mat[15] * this.mat[8] - this.mat[11] * this.mat[12]) + this.mat[7] * (this.mat[10] * this.mat[12] - this.mat[14] * this.mat[8])) + this.mat[2] * (this.mat[4] * (this.mat[15] * this.mat[9] - this.mat[11] * this.mat[13]) + this.mat[5] * (this.mat[11] * this.mat[12] - this.mat[15] * this.mat[8]) + this.mat[7] * (this.mat[13] * this.mat[8] - this.mat[12] * this.mat[9])) + this.mat[3] * (this.mat[4] * (this.mat[10] * this.mat[13] - this.mat[14] * this.mat[9]) + this.mat[5] * (this.mat[14] * this.mat[8] - this.mat[10] * this.mat[12]) + this.mat[6] * (this.mat[12] * this.mat[9] - this.mat[13] * this.mat[8]));
    }

    public Transform inverse()
    {
        float det = this.determinant();

        if (det == 0.0F)
        {
            throw new RuntimeException("Attempting to invert a singular matrix");
        }
        else
        {
            float n00 = this.mat[5] * (this.mat[10] * this.mat[15] - this.mat[11] * this.mat[14]) + this.mat[6] * (this.mat[11] * this.mat[13] - this.mat[15] * this.mat[9]) + this.mat[7] * (this.mat[14] * this.mat[9] - this.mat[10] * this.mat[13]);
            float n01 = this.mat[1] * (this.mat[11] * this.mat[14] - this.mat[10] * this.mat[15]) + this.mat[2] * (this.mat[15] * this.mat[9] - this.mat[11] * this.mat[13]) + this.mat[3] * (this.mat[10] * this.mat[13] - this.mat[14] * this.mat[9]);
            float n02 = this.mat[1] * (this.mat[15] * this.mat[6] - this.mat[14] * this.mat[7]) + this.mat[2] * (this.mat[13] * this.mat[7] - this.mat[15] * this.mat[5]) + this.mat[3] * (this.mat[14] * this.mat[5] - this.mat[13] * this.mat[6]);
            float n03 = this.mat[1] * (this.mat[10] * this.mat[7] - this.mat[11] * this.mat[6]) + this.mat[2] * (this.mat[11] * this.mat[5] - this.mat[7] * this.mat[9]) + this.mat[3] * (this.mat[6] * this.mat[9] - this.mat[10] * this.mat[5]);
            float n04 = this.mat[4] * (this.mat[11] * this.mat[14] - this.mat[10] * this.mat[15]) + this.mat[6] * (this.mat[15] * this.mat[8] - this.mat[11] * this.mat[12]) + this.mat[7] * (this.mat[10] * this.mat[12] - this.mat[14] * this.mat[8]);
            float n05 = this.mat[0] * (this.mat[10] * this.mat[15] - this.mat[11] * this.mat[14]) + this.mat[2] * (this.mat[11] * this.mat[12] - this.mat[15] * this.mat[8]) + this.mat[3] * (this.mat[14] * this.mat[8] - this.mat[10] * this.mat[12]);
            float n06 = this.mat[0] * (this.mat[14] * this.mat[7] - this.mat[15] * this.mat[6]) + this.mat[2] * (this.mat[15] * this.mat[4] - this.mat[12] * this.mat[7]) + this.mat[3] * (this.mat[12] * this.mat[6] - this.mat[14] * this.mat[4]);
            float n07 = this.mat[0] * (this.mat[11] * this.mat[6] - this.mat[10] * this.mat[7]) + this.mat[2] * (this.mat[7] * this.mat[8] - this.mat[11] * this.mat[4]) + this.mat[3] * (this.mat[10] * this.mat[4] - this.mat[6] * this.mat[8]);
            float n08 = this.mat[4] * (this.mat[15] * this.mat[9] - this.mat[11] * this.mat[13]) + this.mat[5] * (this.mat[11] * this.mat[12] - this.mat[15] * this.mat[8]) + this.mat[7] * (this.mat[13] * this.mat[8] - this.mat[12] * this.mat[9]);
            float n09 = this.mat[0] * (this.mat[11] * this.mat[13] - this.mat[15] * this.mat[9]) + this.mat[1] * (this.mat[15] * this.mat[8] - this.mat[11] * this.mat[12]) + this.mat[3] * (this.mat[12] * this.mat[9] - this.mat[13] * this.mat[8]);
            float n10 = this.mat[0] * (this.mat[15] * this.mat[5] - this.mat[13] * this.mat[7]) + this.mat[1] * (this.mat[12] * this.mat[7] - this.mat[15] * this.mat[4]) + this.mat[3] * (this.mat[13] * this.mat[4] - this.mat[12] * this.mat[5]);
            float n11 = this.mat[0] * (this.mat[7] * this.mat[9] - this.mat[11] * this.mat[5]) + this.mat[1] * (this.mat[11] * this.mat[4] - this.mat[7] * this.mat[8]) + this.mat[3] * (this.mat[5] * this.mat[8] - this.mat[4] * this.mat[9]);
            float n12 = this.mat[4] * (this.mat[10] * this.mat[13] - this.mat[14] * this.mat[9]) + this.mat[5] * (this.mat[14] * this.mat[8] - this.mat[10] * this.mat[12]) + this.mat[6] * (this.mat[12] * this.mat[9] - this.mat[13] * this.mat[8]);
            float n13 = this.mat[0] * (this.mat[14] * this.mat[9] - this.mat[10] * this.mat[13]) + this.mat[1] * (this.mat[10] * this.mat[12] - this.mat[14] * this.mat[8]) + this.mat[2] * (this.mat[13] * this.mat[8] - this.mat[12] * this.mat[9]);
            float n14 = this.mat[0] * (this.mat[13] * this.mat[6] - this.mat[14] * this.mat[5]) + this.mat[1] * (this.mat[14] * this.mat[4] - this.mat[12] * this.mat[6]) + this.mat[2] * (this.mat[12] * this.mat[5] - this.mat[13] * this.mat[4]);
            float n15 = this.mat[0] * (this.mat[10] * this.mat[5] - this.mat[6] * this.mat[9]) + this.mat[1] * (this.mat[6] * this.mat[8] - this.mat[10] * this.mat[4]) + this.mat[2] * (this.mat[4] * this.mat[9] - this.mat[5] * this.mat[8]);
            this.mat[0] = n00 / det;
            this.mat[1] = n01 / det;
            this.mat[2] = n02 / det;
            this.mat[3] = n03 / det;
            this.mat[4] = n04 / det;
            this.mat[5] = n05 / det;
            this.mat[6] = n06 / det;
            this.mat[7] = n07 / det;
            this.mat[8] = n08 / det;
            this.mat[9] = n09 / det;
            this.mat[10] = n10 / det;
            this.mat[11] = n11 / det;
            this.mat[12] = n12 / det;
            this.mat[13] = n13 / det;
            this.mat[14] = n14 / det;
            this.mat[15] = n15 / det;
            return this;
        }
    }

    public Transform transpose()
    {
        float temp = 0.0F;
        temp = this.mat[1];
        this.mat[1] = this.mat[4];
        this.mat[4] = temp;
        temp = this.mat[2];
        this.mat[2] = this.mat[8];
        this.mat[8] = temp;
        temp = this.mat[3];
        this.mat[3] = this.mat[12];
        this.mat[12] = temp;
        temp = this.mat[6];
        this.mat[6] = this.mat[9];
        this.mat[9] = temp;
        temp = this.mat[7];
        this.mat[7] = this.mat[13];
        this.mat[13] = temp;
        temp = this.mat[11];
        this.mat[11] = this.mat[14];
        this.mat[14] = temp;
        return this;
    }

    public String toString()
    {
        return String.format("{%#7.4f,%#7.4f,%#7.4f,%#7.4f},\n{%#7.4f,%#7.4f,%#7.4f,%#7.4f},\n{%#7.4f,%#7.4f,%#7.4f,%#7.4f},\n{%#7.4f,%#7.4f,%#7.4f,%#7.4f}", new Object[] {Float.valueOf(this.mat[0]), Float.valueOf(this.mat[1]), Float.valueOf(this.mat[2]), Float.valueOf(this.mat[3]), Float.valueOf(this.mat[4]), Float.valueOf(this.mat[5]), Float.valueOf(this.mat[6]), Float.valueOf(this.mat[7]), Float.valueOf(this.mat[8]), Float.valueOf(this.mat[9]), Float.valueOf(this.mat[10]), Float.valueOf(this.mat[11]), Float.valueOf(this.mat[12]), Float.valueOf(this.mat[13]), Float.valueOf(this.mat[14]), Float.valueOf(this.mat[15])});
    }

    protected static void mult(float[] base, float[] mult)
    {
        float n00 = base[0] * mult[0] + base[1] * mult[4] + base[2] * mult[8] + base[3] * mult[12];
        float n01 = base[0] * mult[1] + base[1] * mult[5] + base[2] * mult[9] + base[3] * mult[13];
        float n02 = base[0] * mult[2] + base[1] * mult[6] + base[2] * mult[10] + base[3] * mult[14];
        float n03 = base[0] * mult[3] + base[1] * mult[7] + base[2] * mult[11] + base[3] * mult[15];
        float n04 = base[4] * mult[0] + base[5] * mult[4] + base[6] * mult[8] + base[7] * mult[12];
        float n05 = base[4] * mult[1] + base[5] * mult[5] + base[6] * mult[9] + base[7] * mult[13];
        float n06 = base[4] * mult[2] + base[5] * mult[6] + base[6] * mult[10] + base[7] * mult[14];
        float n07 = base[4] * mult[3] + base[5] * mult[7] + base[6] * mult[11] + base[7] * mult[15];
        float n08 = base[8] * mult[0] + base[9] * mult[4] + base[10] * mult[8] + base[11] * mult[12];
        float n09 = base[8] * mult[1] + base[9] * mult[5] + base[10] * mult[9] + base[11] * mult[13];
        float n10 = base[8] * mult[2] + base[9] * mult[6] + base[10] * mult[10] + base[11] * mult[14];
        float n11 = base[8] * mult[3] + base[9] * mult[7] + base[10] * mult[11] + base[11] * mult[15];
        float n12 = base[12] * mult[0] + base[13] * mult[4] + base[14] * mult[8] + base[15] * mult[12];
        float n13 = base[12] * mult[1] + base[13] * mult[5] + base[14] * mult[9] + base[15] * mult[13];
        float n14 = base[12] * mult[2] + base[13] * mult[6] + base[14] * mult[10] + base[15] * mult[14];
        float n15 = base[12] * mult[3] + base[13] * mult[7] + base[14] * mult[11] + base[15] * mult[15];
        base[0] = n00;
        base[1] = n01;
        base[2] = n02;
        base[3] = n03;
        base[4] = n04;
        base[5] = n05;
        base[6] = n06;
        base[7] = n07;
        base[8] = n08;
        base[9] = n09;
        base[10] = n10;
        base[11] = n11;
        base[12] = n12;
        base[13] = n13;
        base[14] = n14;
        base[15] = n15;
    }

}
