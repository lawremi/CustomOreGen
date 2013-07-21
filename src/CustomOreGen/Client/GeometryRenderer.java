package CustomOreGen.Client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import CustomOreGen.Util.IGeometryBuilder;
import CustomOreGen.Util.Transform;

public class GeometryRenderer implements IGeometryBuilder
{
    private int _curBufferIdx = -1;
    private Transform _posTrans = null;
    private Transform _nmlTrans = null;
    private float[] _normal = null;
    private float[] _color = null;
    private float[] _texcoords = null;
    private int _texture = -1;
    private Transform _texTrans = null;
    private PrimitiveType _primitive = null;
    private int[] _implicitRefs = null;
    private Map _textureMap = null;
    private ArrayList _vertexBuffers = new ArrayList();
    private VertexBuffer _dbgNormalLines = null;
    private long[] _vertexIndexMap = new long[256];
    private int _vertexCount = 0;
    private int _flushedVertexCount = 0;
    private int _processedVertexCount = 0;
    private boolean _polygonParity = false;

    public void mapTexture(String textureURI, int textureName)
    {
        if (this._textureMap == null)
        {
            this._textureMap = new HashMap();
        }

        if (textureName < 0)
        {
            this._textureMap.remove(textureURI);
        }
        else
        {
            this._textureMap.put(textureURI, Integer.valueOf(textureName));
        }
    }

    public void setPositionTransform(Transform transform)
    {
        this._posTrans = transform;

        if (transform != null && this._normal != null)
        {
            this._nmlTrans = transform.clone().inverse().transpose();
        }
        else
        {
            this._nmlTrans = null;
        }
    }

    public void setNormal(float[] normal)
    {
        if (this._normal == null != (normal == null))
        {
            this._curBufferIdx = -1;

            if (normal == null)
            {
                this._nmlTrans = null;
            }
            else if (this._posTrans != null)
            {
                this._nmlTrans = this._posTrans.clone().inverse().transpose();
            }
        }

        this._normal = normal;
    }

    public void setColor(float[] color)
    {
        if (this._color == null != (color == null))
        {
            this._curBufferIdx = -1;
        }

        this._color = color;
    }

    public void setTexture(String textureURI)
    {
        int texture = -1;

        if (textureURI != null && this._textureMap != null)
        {
            Integer value = (Integer)this._textureMap.get(textureURI);

            if (value != null)
            {
                texture = value.intValue();
            }
        }

        if (this._texcoords != null && this._texture != texture)
        {
            this._curBufferIdx = -1;
        }

        this._texture = texture;
    }

    public void setTextureTransform(Transform transform)
    {
        this._texTrans = transform;
    }

    public void setTextureCoordinates(float[] texcoords)
    {
        if (this._texture >= 0 && this._texcoords == null != (texcoords == null))
        {
            this._curBufferIdx = -1;
        }

        this._texcoords = texcoords;
    }

    public void setVertexMode(PrimitiveType primitive, int ... vertexIndices)
    {
        if (this._primitive != primitive)
        {
            this._curBufferIdx = -1;
        }

        this._primitive = primitive;
        int curIRefCount = this._implicitRefs == null ? 0 : this._implicitRefs.length;
        int newIRefCount = vertexIndices == null ? 0 : vertexIndices.length;

        if (vertexIndices != this._implicitRefs && (curIRefCount > 0 || newIRefCount > 0) && !Arrays.equals(vertexIndices, this._implicitRefs))
        {
            ;
        }

        this._implicitRefs = vertexIndices;
        this._polygonParity = false;
        this._processedVertexCount = this._vertexCount;
        this._flushedVertexCount = this._vertexCount;
    }

    public void addVertex(float[] pos)
    {
        this.addVertex(pos, (float[])null, (float[])null, (float[])null);
    }

    public void addVertex(float[] pos, float[] normal, float[] color, float[] texcoords)
    {
        if (this._curBufferIdx < 0)
        {
            this.setVertexBuffer();
        }

        VertexBuffer curBuffer = (VertexBuffer)this._vertexBuffers.get(this._curBufferIdx);
        float[][] args = new float[4][];
        byte argIdx = 0;

        if (this._posTrans != null)
        {
            pos = Arrays.copyOf(pos, 3);
            this._posTrans.transformVector(pos);
        }

        int var10 = argIdx + 1;
        args[argIdx] = pos;

        if (this._normal != null)
        {
            if (normal == null)
            {
                normal = this._normal;
            }

            if (this._nmlTrans != null)
            {
                normal = Arrays.copyOf(normal, 3);
                this._nmlTrans.transformVector(normal);
            }

            args[var10++] = normal;

            if (this._dbgNormalLines != null)
            {
                normal = Arrays.copyOf(normal, 3);
                normal[0] *= 0.1F;
                normal[1] *= 0.1F;
                normal[2] *= 0.1F;
                normal[0] += pos[0];
                normal[1] += pos[1];
                normal[2] += pos[2];
                this._dbgNormalLines.addIndex(this._dbgNormalLines.addVertex(new float[][] {pos}));
                this._dbgNormalLines.addIndex(this._dbgNormalLines.addVertex(new float[][] {normal}));
            }
        }

        if (this._color != null)
        {
            if (color == null)
            {
                color = this._color;
            }

            args[var10++] = color;
        }

        if (this._texture >= 0 && this._texcoords != null)
        {
            if (texcoords == null)
            {
                texcoords = this._texcoords;
            }

            if (this._texTrans != null)
            {
                texcoords = Arrays.copyOf(texcoords, 2);
                this._texTrans.transformVector(texcoords);
            }

            args[var10++] = texcoords;
        }

        int vidx = curBuffer.addVertex(args);

        if (this._vertexCount == this._vertexIndexMap.length)
        {
            this._vertexIndexMap = Arrays.copyOf(this._vertexIndexMap, this._vertexCount * 2);
        }

        this._vertexIndexMap[this._vertexCount] = (long)this._curBufferIdx << 32 | (long)vidx;
        ++this._vertexCount;
        this.processVertices();
    }

    public void addVertexRef(int vertexIndex)
    {
        if (this._curBufferIdx < 0)
        {
            this.setVertexBuffer();
        }

        if (this._vertexCount == this._vertexIndexMap.length)
        {
            this._vertexIndexMap = Arrays.copyOf(this._vertexIndexMap, this._vertexCount * 2);
        }

        if (vertexIndex >= 1 && vertexIndex <= this._vertexCount)
        {
            this._vertexIndexMap[this._vertexCount] = this._vertexIndexMap[this._vertexCount - vertexIndex];
        }
        else
        {
            this._vertexIndexMap[this._vertexCount] = -1L;
        }

        ++this._vertexCount;
        this.processVertices();
    }

    public void draw()
    {
        Iterator i$ = this._vertexBuffers.iterator();

        while (i$.hasNext())
        {
            VertexBuffer buffer = (VertexBuffer)i$.next();
            buffer.drawBuffer();
        }

        if (this._dbgNormalLines != null)
        {
            this._dbgNormalLines.drawBuffer();
        }
    }

    public void enableDebuggingNormalLines(boolean enable)
    {
        if (!enable)
        {
            this._dbgNormalLines = null;
        }
        else if (this._dbgNormalLines == null)
        {
            this._dbgNormalLines = new VertexBuffer(128, 1, false, false, -1);
        }
    }

    private void processVertices()
    {
        VertexBuffer curBuffer = this._curBufferIdx >= 0 ? (VertexBuffer)this._vertexBuffers.get(this._curBufferIdx) : null;

        if (curBuffer != null && this._primitive != null)
        {
            byte vCount = 0;

            switch (this._primitive)
            {
                case POINT:
                    vCount = 1;
                    break;

                case LINE:
                    vCount = 2;
                    break;

                case TRIANGLE:
                case TRIANGLE_ALT:
                    vCount = 3;
                    break;

                case QUAD:
                    vCount = 4;
            }

            int iRefCount = this._implicitRefs == null ? 0 : this._implicitRefs.length;

            if (iRefCount >= vCount)
            {
                iRefCount = vCount - 1;
            }

            int iRefMax = 0;
            int groupSize;

            for (groupSize = 0; groupSize < iRefCount; ++groupSize)
            {
                if (this._implicitRefs[groupSize] > iRefMax)
                {
                    iRefMax = this._implicitRefs[groupSize];
                }
            }

            if (this._processedVertexCount - this._flushedVertexCount < iRefMax)
            {
                this._processedVertexCount = Math.min(this._flushedVertexCount + iRefMax, this._vertexCount);
            }

            groupSize = vCount - iRefCount;

            while (this._vertexCount - this._processedVertexCount >= groupSize)
            {
                VertexBuffer[] polyVertBuffers = new VertexBuffer[vCount];
                int[] polyVertIndices = new int[vCount];
                boolean valid = true;
                int i = 0;

                while (true)
                {
                    if (i < vCount)
                    {
                        long j = -1L;

                        if (i < iRefCount)
                        {
                            if (this._implicitRefs[i] >= 1)
                            {
                                j = this._vertexIndexMap[this._processedVertexCount - this._implicitRefs[i]];
                            }
                        }
                        else
                        {
                            j = this._vertexIndexMap[this._processedVertexCount + (i - iRefCount)];
                        }

                        if (j == -1L)
                        {
                            valid = false;
                        }
                        else
                        {
                            polyVertBuffers[i] = (VertexBuffer)this._vertexBuffers.get((int)(j >>> 32));

                            if (!curBuffer.canCopyFrom(polyVertBuffers[i]))
                            {
                                valid = false;
                            }
                            else
                            {
                                polyVertIndices[i] = (int)(j & -1L);

                                if (polyVertIndices[i] >= 0 && polyVertIndices[i] < polyVertBuffers[i].getVertexCount())
                                {
                                    ++i;
                                    continue;
                                }

                                valid = false;
                            }
                        }
                    }

                    if (valid)
                    {
                        for (i = 0; i < vCount; ++i)
                        {
                            int var12 = this._primitive == PrimitiveType.TRIANGLE_ALT && this._polygonParity ? vCount - 1 - i : i;

                            if (polyVertBuffers[var12] != curBuffer)
                            {
                                polyVertIndices[var12] = curBuffer.copyVertex(polyVertBuffers[var12], polyVertIndices[var12]);
                            }

                            curBuffer.addIndex(polyVertIndices[var12]);
                        }

                        this._polygonParity = !this._polygonParity;
                    }

                    this._processedVertexCount += groupSize;
                    break;
                }
            }
        }
    }

    private void setVertexBuffer()
    {
        byte renderMode = -1;

        if (this._primitive != null)
        {
            switch(this._primitive)
            {
                case POINT:
                    renderMode = 0;
                    break;

                case LINE:
                    renderMode = 1;
                    break;

                case TRIANGLE:
                case TRIANGLE_ALT:
                    renderMode = 4;
                    break;

                case QUAD:
                    renderMode = 7;
            }
        }

        boolean hasNormal = this._normal != null;
        boolean hasColor = this._color != null;
        int texture = this._texcoords == null ? -1 : this._texture;
        VertexBuffer curBuffer = this._curBufferIdx >= 0 ? (VertexBuffer)this._vertexBuffers.get(this._curBufferIdx) : null;

        if (curBuffer == null || renderMode != curBuffer.renderMode || hasNormal != curBuffer.hasNormal || hasColor != curBuffer.hasColor || texture != curBuffer.texture)
        {
            VertexBuffer newBuffer = null;

            for (int i = 0; i < this._vertexBuffers.size(); ++i)
            {
                VertexBuffer buffer = (VertexBuffer)this._vertexBuffers.get(i);

                if (buffer != curBuffer && renderMode == buffer.renderMode && hasNormal == buffer.hasNormal && hasColor == buffer.hasColor && texture == buffer.texture)
                {
                    this._curBufferIdx = i;
                    newBuffer = buffer;
                    break;
                }
            }

            if (newBuffer == null)
            {
                newBuffer = new VertexBuffer(renderMode > 0 ? 128 : 0, renderMode, hasNormal, hasColor, texture);
                this._curBufferIdx = this._vertexBuffers.size();
                this._vertexBuffers.add(newBuffer);
            }
        }
    }
    
    public class VertexBuffer
    {
        public final int renderMode;
        public final boolean hasNormal;
        public final boolean hasColor;
        public final int texture;
        private ByteBuffer vBuffer;
        private IntBuffer xBuffer;

        public VertexBuffer(int initialSize, int renderMode, boolean hasNormal, boolean hasColor, int texture)
        {
            this.renderMode = renderMode;
            this.hasNormal = hasNormal;
            this.hasColor = hasColor;
            this.texture = texture;

            if (initialSize <= 0)
            {
                initialSize = 1;
            }

            this.vBuffer = ByteBuffer.allocateDirect(initialSize * this.getVertexSize()).order(ByteOrder.nativeOrder());
            this.xBuffer = ByteBuffer.allocateDirect(initialSize * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            this.clear();
        }

        public void clear()
        {
            this.vBuffer.limit(0);
            this.xBuffer.limit(0);
        }

        private int getVertexSize()
        {
            int vsize = 3;

            if (this.hasNormal)
            {
                vsize += 3;
            }

            if (this.hasColor)
            {
                ++vsize;
            }

            if (this.texture >= 0)
            {
                vsize += 2;
            }

            return vsize * 4;
        }

        public int getVertexCount()
        {
            return this.vBuffer.limit() / this.getVertexSize();
        }

        public int addVertex(float[] ... data)
        {
            int offset = this.vBuffer.limit();
            int vsize = this.getVertexSize();

            if (offset + vsize > this.vBuffer.capacity())
            {
                ByteBuffer dataIdx = this.vBuffer;
                this.vBuffer = ByteBuffer.allocateDirect(dataIdx.capacity() * 2).order(ByteOrder.nativeOrder());
                dataIdx.rewind();
                this.vBuffer.put(dataIdx);
            }

            this.vBuffer.limit(offset + vsize);
            this.vBuffer.position(offset);
            byte var9 = 0;
            this.vBuffer.putFloat(data[var9][0]);
            this.vBuffer.putFloat(data[var9][1]);
            this.vBuffer.putFloat(data[var9][2]);
            int var10 = var9 + 1;

            if (this.hasNormal)
            {
                this.vBuffer.putFloat(data[var10][0]);
                this.vBuffer.putFloat(data[var10][1]);
                this.vBuffer.putFloat(data[var10][2]);
                ++var10;
            }

            if (this.hasColor)
            {
                int r = (int)((double)(data[var10][0] * 255.0F) + 0.5D);

                if (r > 255)
                {
                    r = 255;
                }
                else if (r < 0)
                {
                    r = 0;
                }

                int g = (int)((double)(data[var10][1] * 255.0F) + 0.5D);

                if (g > 255)
                {
                    g = 255;
                }
                else if (g < 0)
                {
                    g = 0;
                }

                int b = (int)((double)(data[var10][2] * 255.0F) + 0.5D);

                if (b > 255)
                {
                    b = 255;
                }
                else if (b < 0)
                {
                    b = 0;
                }

                int a = 255;

                if (data[var10].length > 3)
                {
                    a = (int)((double)(data[var10][3] * 255.0F) + 0.5D);

                    if (a > 255)
                    {
                        a = 255;
                    }
                    else if (a < 0)
                    {
                        a = 0;
                    }
                }

                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
                {
                    this.vBuffer.putInt(a << 24 | b << 16 | g << 8 | r);
                }
                else
                {
                    this.vBuffer.putInt(r << 24 | g << 16 | b << 8 | a);
                }

                ++var10;
            }

            if (this.texture >= 0)
            {
                this.vBuffer.putFloat(data[var10][0]);
                this.vBuffer.putFloat(data[var10][1]);
                ++var10;
            }

            return offset / vsize;
        }

        public boolean canCopyFrom(VertexBuffer sourceBuffer)
        {
            return sourceBuffer == null ? false : (this.hasNormal && !sourceBuffer.hasNormal ? false : (this.hasColor && !sourceBuffer.hasColor ? false : this.texture < 0 || this.texture == sourceBuffer.texture));
        }

        public int copyVertex(VertexBuffer sourceBuffer, int sourceIndex)
        {
            if (!this.canCopyFrom(sourceBuffer))
            {
                return -1;
            }
            else if (sourceIndex >= 0 && sourceIndex <= sourceBuffer.getVertexCount())
            {
                byte[] tmp = new byte[16];
                sourceBuffer.vBuffer.position(sourceIndex * sourceBuffer.getVertexSize());
                int offset = this.vBuffer.limit();
                int vsize = this.getVertexSize();

                if (offset + vsize > this.vBuffer.capacity())
                {
                    ByteBuffer oldBuffer = this.vBuffer;
                    this.vBuffer = ByteBuffer.allocateDirect(oldBuffer.capacity() * 2).order(ByteOrder.nativeOrder());
                    oldBuffer.rewind();
                    this.vBuffer.put(oldBuffer);
                }

                this.vBuffer.limit(offset + vsize);
                this.vBuffer.position(offset);
                sourceBuffer.vBuffer.get(tmp, 0, 12);
                this.vBuffer.put(tmp, 0, 12);

                if (sourceBuffer.hasNormal)
                {
                    sourceBuffer.vBuffer.get(tmp, 0, 12);
                }

                if (this.hasNormal)
                {
                    this.vBuffer.put(tmp, 0, 12);
                }

                if (sourceBuffer.hasColor)
                {
                    sourceBuffer.vBuffer.get(tmp, 0, 4);
                }

                if (this.hasColor)
                {
                    this.vBuffer.put(tmp, 0, 4);
                }

                if (sourceBuffer.texture >= 0)
                {
                    sourceBuffer.vBuffer.get(tmp, 0, 8);
                }

                if (this.texture >= 0)
                {
                    this.vBuffer.put(tmp, 0, 8);
                }

                return offset / vsize;
            }
            else
            {
                return -1;
            }
        }

        public int getIndexCount()
        {
            return this.xBuffer.limit();
        }

        public void addIndex(int index)
        {
            int offset = this.xBuffer.limit();

            if (offset == this.xBuffer.capacity())
            {
                IntBuffer oldBuffer = this.xBuffer;
                this.xBuffer = ByteBuffer.allocateDirect(oldBuffer.capacity() * 8).order(ByteOrder.nativeOrder()).asIntBuffer();
                oldBuffer.rewind();
                this.xBuffer.put(oldBuffer);
            }

            this.xBuffer.limit(offset + 1);
            this.xBuffer.position(offset);
            this.xBuffer.put(index);
        }

        public void drawBuffer()
        {
            int vCount = this.getVertexCount();
            int vsize = this.getVertexSize();
            int xCount = this.getIndexCount();

            if (this.renderMode >= 0 && vCount != 0 && xCount != 0)
            {
                byte offset = 0;
                this.vBuffer.position(offset);
                GL11.glVertexPointer(3, vsize, this.vBuffer.asFloatBuffer());
                GL11.glEnableClientState(32884);
                int offset1 = offset + 12;

                if (this.hasNormal)
                {
                    this.vBuffer.position(offset1);
                    GL11.glNormalPointer(vsize, this.vBuffer.asFloatBuffer());
                    GL11.glEnableClientState(32885);
                    offset1 += 12;
                }

                if (this.hasColor)
                {
                    this.vBuffer.position(offset1);
                    GL11.glColorPointer(4, true, vsize, this.vBuffer);
                    GL11.glEnableClientState(32886);
                    offset1 += 4;
                }

                if (this.texture >= 0)
                {
                    this.vBuffer.position(offset1);
                    GL11.glTexCoordPointer(2, vsize, this.vBuffer.asFloatBuffer());
                    GL11.glEnableClientState(32888);
                    offset1 += 8;
                }

                if (this.texture >= 0 && this.texture != GL11.glGetInteger(32873))
                {
                    GL11.glBindTexture(3553, this.texture);
                }

                this.xBuffer.rewind();
                GL11.glDrawElements(this.renderMode, this.xBuffer);
                GL11.glDisableClientState(32884);

                if (this.hasNormal)
                {
                    GL11.glDisableClientState(32885);
                }

                if (this.hasColor)
                {
                    GL11.glDisableClientState(32886);
                }

                if (this.texture >= 0)
                {
                    GL11.glDisableClientState(32888);
                }
            }
        }
    }

}
