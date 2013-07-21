package CustomOreGen.Util;


public interface IGeometryBuilder
{
    void setPositionTransform(Transform var1);

    void setNormal(float[] var1);

    void setColor(float[] var1);

    void setTexture(String var1);

    void setTextureTransform(Transform var1);

    void setTextureCoordinates(float[] var1);

    void setVertexMode(PrimitiveType var1, int ... var2);

    void addVertex(float[] var1);

    void addVertex(float[] var1, float[] var2, float[] var3, float[] var4);

    void addVertexRef(int var1);
    
    public enum PrimitiveType
    {
        POINT,
        LINE,
        TRIANGLE,
        TRIANGLE_ALT,
        QUAD;
    }
}
