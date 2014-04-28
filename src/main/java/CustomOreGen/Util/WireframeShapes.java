package CustomOreGen.Util;

import java.util.Hashtable;
import java.util.Map;

import CustomOreGen.Util.IGeometryBuilder.PrimitiveType;

public class WireframeShapes
{
    private static float[][] _cubePoints = new float[][] {{1.0F, -1.0F, 1.0F}, { -1.0F, -1.0F, 1.0F}, {1.0F, 1.0F, 1.0F}, { -1.0F, 1.0F, 1.0F}, {1.0F, 1.0F, -1.0F}, { -1.0F, 1.0F, -1.0F}, {1.0F, -1.0F, -1.0F}, { -1.0F, -1.0F, -1.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}, {0.0F, 0.0F, 0.0F}};
    private static Map _circlePointCache;

    public static void addUnitWireCube(IGeometryBuilder builder)
    {
        builder.setVertexMode(PrimitiveType.QUAD, new int[] {1, 2});
        builder.addVertex(_cubePoints[0], _cubePoints[8], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[1], _cubePoints[9], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[2], _cubePoints[10], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[3], _cubePoints[11], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[4], _cubePoints[12], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[5], _cubePoints[13], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[6], _cubePoints[14], (float[])null, (float[])null);
        builder.addVertex(_cubePoints[7], _cubePoints[15], (float[])null, (float[])null);
        builder.setVertexMode(PrimitiveType.QUAD, new int[] {1, 2});
        builder.addVertexRef(6);
        builder.addVertexRef(5);
        builder.addVertexRef(10);
        builder.addVertexRef(5);
        builder.addVertexRef(11);
        builder.addVertexRef(6);
        builder.addVertexRef(11);
        builder.addVertexRef(10);
    }

    public static void addUnitWireSphere(IGeometryBuilder builder, int segments, int stacks)
    {
        if (segments >= 2 && stacks >= 2)
        {
            builder.setVertexMode(PrimitiveType.QUAD, new int[] {segments + 1, segments + 2, 1});
            float[][] xycoords = getCirclePoints(segments, (float[][])null);
            float[][] zcoords = getCirclePoints(2 * stacks, (float[][])null);
            float[] pos = new float[3];
            int offset;
            int s;

            for (offset = 1; offset < stacks; ++offset)
            {
                for (s = 0; s < segments; ++s)
                {
                    pos[0] = zcoords[offset][1] * xycoords[s][0];
                    pos[1] = zcoords[offset][1] * xycoords[s][1];
                    pos[2] = zcoords[offset][0];
                    builder.addVertex(pos, pos, (float[])null, (float[])null);
                }

                builder.addVertexRef(segments);
            }

            builder.setVertexMode(PrimitiveType.TRIANGLE, new int[] {1});
            pos[0] = pos[1] = 0.0F;
            pos[2] = 1.0F;
            offset = (segments + 1) * (stacks - 2) + 1;
            builder.addVertexRef(offset);

            for (s = 1; s <= segments; ++s)
            {
                if (s == 1)
                {
                    builder.addVertex(pos, pos, (float[])null, (float[])null);
                }
                else
                {
                    builder.addVertexRef(2);
                }

                builder.addVertexRef(offset + 3 * s);
            }

            builder.setVertexMode(PrimitiveType.TRIANGLE, new int[] {1});
            pos[0] = pos[1] = 0.0F;
            pos[2] = -1.0F;
            offset = 3 * segments + 2;
            builder.addVertexRef(offset);

            for (s = 1; s <= segments; ++s)
            {
                if (s == 1)
                {
                    builder.addVertex(pos, pos, (float[])null, (float[])null);
                }
                else
                {
                    builder.addVertexRef(2);
                }

                builder.addVertexRef(offset + s);
            }
        }
    }

    public static void addUnitMercatorSphere(IGeometryBuilder builder, int segments, int stacks)
    {
        if (segments >= 2 && stacks >= 2)
        {
            builder.setVertexMode(PrimitiveType.QUAD, new int[] {segments + 1, segments + 2, 1});
            float[][] xycoords = getCirclePoints(segments, (float[][])null);
            float[][] zcoords = getCirclePoints(2 * stacks, (float[][])null);
            float[] pos = new float[3];
            float[] tex = new float[2];

            for (int t = 0; t <= stacks; ++t)
            {
                double tmp = Math.log((double)((1.0F + zcoords[t][0]) / zcoords[t][1]));
                tex[1] = (float)Math.max(0.0D, Math.min(1.0D, tmp / 4.8725D + 0.5D));

                for (int s = 0; s < segments; ++s)
                {
                    pos[0] = zcoords[t][1] * xycoords[s][0];
                    pos[1] = zcoords[t][1] * xycoords[s][1];
                    pos[2] = zcoords[t][0];
                    tex[0] = (float)s / (float)segments;
                    builder.addVertex(pos, pos, (float[])null, tex);
                }

                pos[0] = zcoords[t][1] * xycoords[0][0];
                pos[1] = zcoords[t][1] * xycoords[0][1];
                pos[2] = zcoords[t][0];
                tex[0] = 1.0F;
                builder.addVertex(pos, pos, (float[])null, tex);
            }
        }
    }

    public static void addUnitCircle(IGeometryBuilder builder, int segments)
    {
        if (segments >= 2)
        {
            float[][] pts = getCirclePoints(segments, (float[][])null);
            builder.setVertexMode(PrimitiveType.LINE, new int[] {1});

            for (int s = 0; s < segments; ++s)
            {
                builder.addVertex(pts[s], pts[s], (float[])null, (float[])null);
            }

            builder.addVertexRef(segments);
        }
    }

    public static float[][] getCirclePoints(int segments, float[][] output)
    {
        if (output == null)
        {
            output = (float[][])_circlePointCache.get(Integer.valueOf(segments));

            if (output != null)
            {
                return output;
            }

            output = new float[segments][3];
            _circlePointCache.put(Integer.valueOf(segments), output);
        }

        float da = ((float)Math.PI * 2F) / (float)segments;
        float angle = 0.0F;

        for (int s = 0; s < segments; ++s)
        {
            output[s][0] = (float)Math.cos((double)angle);
            output[s][1] = (float)Math.sin((double)angle);
            output[s][2] = 0.0F;
            angle += da;
        }

        return output;
    }

    static
    {
        for (int i = 0; i < 24; ++i)
        {
            _cubePoints[8 + i % 8][i / 8] = _cubePoints[i % 8][i / 8] * (float)Math.sqrt(3.0D);
        }

        _circlePointCache = new Hashtable();
    }
}
