package CustomOreGen.Util;


public interface IGeometryBuilder
{
    void setPositionTransform(Transform transform);

    void setNormal(float[] normal);

    void setColor(float[] color);

    void setTexture(String texture);

    void setTextureTransform(Transform transform);

    void setTextureCoordinates(float[] texcoords);

    void setVertexMode(PrimitiveType primitive, int ... vertexIndices);

    void addVertex(float[] pos);

    void addVertex(float[] pos, float[] normal, float[] color, float[] texcoords);

    void addVertexRef(int vertexIndex);
    
    public enum PrimitiveType
    {
        POINT,
        LINE,
        TRIANGLE,
        TRIANGLE_ALT,
        QUAD;
    }
}
