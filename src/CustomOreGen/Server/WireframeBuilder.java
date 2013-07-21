package CustomOreGen.Server;

import CustomOreGen.Util.Transform;

public class WireframeBuilder
{
    public static WireframeBuilder instance = new WireframeBuilder();
    public long quads = 0L;
    public long lines = 0L;
    protected final Transform defaultTrans = new Transform();

    public int startDisplayList()
    {
        this.quads = this.lines = 0L;
        return 1;
    }

    public void endDisplayList() {}

    public void deleteDisplayList(int listID) {}

    public void setColor(long color) {}

    public void setTransform(Transform transform) {}

    public Transform getDefaultTransform()
    {
        return this.defaultTrans;
    }

    public void addLine(float aX, float aY, float aZ, float bX, float bY, float bZ)
    {
        ++this.lines;
    }

    public void addQuad(float aX, float aY, float aZ, float bX, float bY, float bZ, float cX, float cY, float cZ, float dX, float dY, float dZ)
    {
        ++this.quads;
    }

    public void addQuadStrip(float[] left, float[] right, int count, boolean closed)
    {
        this.quads += (long)(count - 1);
    }

    public void addCube()
    {
        this.quads += 6L;
    }

    public void addCircle(int segments)
    {
        this.lines += (long)segments;
    }

    public void addSphere(int segments, int stacks)
    {
        this.quads += (long)(segments * stacks);
    }

    public void getCirclePoints(float[] points, int segments, float radius, float z) {}
}
