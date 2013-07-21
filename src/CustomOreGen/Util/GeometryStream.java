package CustomOreGen.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class GeometryStream implements IGeometryBuilder
{
    private static final float[] _normalVectorTable = new float[6240];
    private byte[] _stream = null;
    private int _readPos = 0;
    private int _endPos = 0;
    private int _flags = 0;

    public GeometryStream() {}

    public GeometryStream(InputStream input) throws IOException
    {
        if (input != null)
        {
            int streamSize = input.read();
            streamSize |= input.read() << 8;
            streamSize |= input.read() << 16;
            streamSize |= input.read() << 24;

            if (streamSize > 0)
            {
                this._stream = new byte[Integer.highestOneBit(streamSize * 2 - 1)];

                for (int read = 0; read < streamSize; read += input.read(this._stream, read, streamSize - read))
                {
                    ;
                }

                this._endPos = streamSize * 8;
                this._readPos = 0;
            }
        }
    }

    public void setPositionTransform(Transform transform)
    {
        if (transform != null)
        {
            this.packBits(15, 6);

            for (int i = 0; i < 12; ++i)
            {
                this.packBits(Float.floatToRawIntBits(transform.element(i / 4, i % 4)), 32);
            }
        }
        else
        {
            this.packBits(14, 6);
        }
    }

    public void setNormal(float[] normal)
    {
        if (normal != null)
        {
            this.packBits(13, 6);
            this.packBits(compressNormalVector(normal[0], normal[1], normal[2]), 18);
        }
        else
        {
            this.packBits(12, 6);
        }
    }

    public void setColor(float[] color)
    {
        if (color != null)
        {
            this.packBits(11, 6);
            this.packBits(to32BitColor(color[0], color[1], color[2], color.length > 3 ? color[3] : 1.0F), 32);
        }
        else
        {
            this.packBits(10, 6);
        }
    }

    public void setTexture(String textureURI)
    {
        if (textureURI != null)
        {
            if (textureURI.length() > 65535)
            {
                throw new IllegalArgumentException("Texture URIs longer than 65,535 characters are not supported");
            }

            this.packBits(39, 6);
            this.packBits(textureURI.length(), 16);

            for (int c = 0; c < textureURI.length(); ++c)
            {
                this.packBits(textureURI.charAt(c), 8);
            }
        }
        else
        {
            this.packBits(36, 6);
        }
    }

    public void setTextureTransform(Transform transform)
    {
        if (transform != null)
        {
            this.packBits(9, 6);

            for (int i = 0; i < 12; ++i)
            {
                this.packBits(Float.floatToRawIntBits(transform.element(i / 4, i % 4)), 32);
            }
        }
        else
        {
            this.packBits(8, 6);
        }
    }

    public void setTextureCoordinates(float[] texcoords)
    {
        if (texcoords != null)
        {
            int opCode = 34;
            boolean fullPrec = this.useFullTexPrecision(texcoords);

            if (fullPrec)
            {
                opCode |= 1;
            }

            this.packBits(opCode, 6);

            if (fullPrec)
            {
                this.packBits(Float.floatToRawIntBits(texcoords[0]), 32);
                this.packBits(Float.floatToRawIntBits(texcoords[1]), 32);
            }
            else
            {
                this.packBits(toHalfFloat(texcoords[0]), 16);
                this.packBits(toHalfFloat(texcoords[1]), 16);
            }
        }
        else
        {
            this.packBits(32, 6);
        }
    }

    public void setVertexMode(PrimitiveType primitive, int ... vertexIndices)
    {
        int opcode = 16;
        byte irefMax = 0;

        if (primitive != null)
        {
            switch (primitive)
            {
                case POINT:
                    opcode |= 1;
                    break;

                case LINE:
                    opcode |= 2;
                    irefMax = 1;
                    break;

                case TRIANGLE:
                    opcode |= 4;
                    irefMax = 2;
                    break;

                case TRIANGLE_ALT:
                    opcode |= 8;
                    irefMax = 2;
                    break;

                case QUAD:
                    opcode |= 12;
                    irefMax = 3;
            }
        }

        int irefCount = Math.min(irefMax, vertexIndices.length);
        this.packBits(opcode | irefCount, 6);

        for (int i = 0; i < irefCount; ++i)
        {
            if (vertexIndices[i] > 65535)
            {
                throw new IllegalArgumentException("Vertex indices larger than 65,535 are not supported");
            }

            this.packBits(vertexIndices[i], 16);
        }
    }

    public void addVertex(float[] pos)
    {
        this.addVertex(pos, (float[])null, (float[])null, (float[])null);
    }

    public void addVertex(float[] pos, float[] normal, float[] color, float[] texcoords)
    {
        boolean fullPosPrec = this.useFullPosPrecision(pos);
        boolean fullTexPrec = false;
        byte opCode = 32;
        int opCode1;

        if (texcoords != null)
        {
            opCode1 = opCode | 16;
            fullTexPrec = this.useFullTexPrecision(texcoords);

            if (fullTexPrec)
            {
                opCode1 |= 8;
            }
        }
        else
        {
            opCode1 = opCode | 8;
        }

        if (color != null)
        {
            opCode1 |= 4;
        }

        if (normal != null)
        {
            opCode1 |= 2;
        }

        if (fullPosPrec)
        {
            opCode1 |= 1;
        }

        this.packBits(opCode1, 6);

        if (fullPosPrec)
        {
            this.packBits(Float.floatToRawIntBits(pos[0]), 32);
            this.packBits(Float.floatToRawIntBits(pos[1]), 32);
            this.packBits(Float.floatToRawIntBits(pos[2]), 32);
        }
        else
        {
            this.packBits(toHalfFloat(pos[0]), 16);
            this.packBits(toHalfFloat(pos[1]), 16);
            this.packBits(toHalfFloat(pos[2]), 16);
        }

        if (normal != null)
        {
            this.packBits(compressNormalVector(normal[0], normal[1], normal[2]), 18);
        }

        if (color != null)
        {
            this.packBits(to32BitColor(color[0], color[1], color[2], color.length > 3 ? color[3] : 1.0F), 32);
        }

        if (texcoords != null)
        {
            if (fullTexPrec)
            {
                this.packBits(Float.floatToRawIntBits(texcoords[0]), 32);
                this.packBits(Float.floatToRawIntBits(texcoords[1]), 32);
            }
            else
            {
                this.packBits(toHalfFloat(texcoords[0]), 16);
                this.packBits(toHalfFloat(texcoords[1]), 16);
            }
        }
    }

    public void addVertexRef(int vertexIndex)
    {
        if (vertexIndex < 256)
        {
            this.packBits(6, 6);
            this.packBits(vertexIndex, 8);
        }
        else
        {
            this.packBits(7, 6);
            this.packBits(vertexIndex, 32);
        }
    }

    private void execSetPositionTransform(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            Transform trans = new Transform();

            for (int i = 0; i < 12; ++i)
            {
                trans.setElement(i / 4, i % 4, Float.intBitsToFloat(this.unpackBits(32)));
            }

            target.setPositionTransform(trans);
        }
        else
        {
            target.setPositionTransform((Transform)null);
        }
    }

    private void execSetNormal(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            target.setNormal(decompressNormalVector(this.unpackBits(18), (float[])null));
        }
        else
        {
            target.setNormal((float[])null);
        }
    }

    private void execSetColor(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            target.setColor(toFloatColor(this.unpackBits(32), (float[])null));
        }
        else
        {
            target.setColor((float[])null);
        }
    }

    private void execSetTexture(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 3) != 0)
        {
            if ((opcode & 3) != 3)
            {
                throw new GeometryStreamException("Unexpected SetTexture mode (" + opcode + ").");
            }

            int length = this.unpackBits(16);
            StringBuilder str = new StringBuilder(length);

            for (int i = 0; i < length; ++i)
            {
                str.append((char)this.unpackBits(8));
            }

            target.setTexture(str.toString());
        }
        else
        {
            target.setTexture((String)null);
        }
    }

    private void execSetTextureTransform(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            Transform trans = new Transform();

            for (int i = 0; i < 12; ++i)
            {
                trans.setElement(i / 4, i % 4, Float.intBitsToFloat(this.unpackBits(32)));
            }

            target.setTextureTransform(trans);
        }
        else
        {
            target.setTextureTransform((Transform)null);
        }
    }

    private void execSetTextureCoordinates(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 2) != 0)
        {
            float[] coords = new float[2];

            if ((opcode & 1) != 0)
            {
                coords[0] = Float.intBitsToFloat(this.unpackBits(32));
                coords[1] = Float.intBitsToFloat(this.unpackBits(32));
            }
            else
            {
                coords[0] = fromHalfFloat(this.unpackBits(16));
                coords[1] = fromHalfFloat(this.unpackBits(16));
            }

            target.setTextureCoordinates(coords);
        }
        else
        {
            if ((opcode & 1) != 0)
            {
                throw new GeometryStreamException("Unexpected SetTextureCoordinates mode (" + opcode + ").");
            }

            target.setTextureCoordinates((float[])null);
        }
    }

    private void execSetVertexMode(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        PrimitiveType primitive = null;
        int irefCount = 0;

        if ((opcode & 8) != 0)
        {
            if ((opcode & 4) != 0)
            {
                primitive = PrimitiveType.QUAD;
                irefCount = opcode & 3;
            }
            else
            {
                primitive = PrimitiveType.TRIANGLE;
                irefCount = opcode & 3;

                if (irefCount == 3)
                {
                    throw new IllegalArgumentException("Triangle mode may not specify more than 3 implict references!");
                }
            }
        }
        else if ((opcode & 4) != 0)
        {
            primitive = PrimitiveType.TRIANGLE_ALT;
            irefCount = opcode & 3;

            if (irefCount == 3)
            {
                throw new IllegalArgumentException("Triangle mode may not specify more than 3 implict references!");
            }
        }
        else if ((opcode & 2) != 0)
        {
            primitive = PrimitiveType.LINE;
            irefCount = opcode & 1;
        }
        else if ((opcode & 1) != 0)
        {
            primitive = PrimitiveType.POINT;
        }

        int[] irefs = new int[irefCount];

        for (int i = 0; i < irefCount; ++i)
        {
            irefs[i] = this.unpackBits(16);
        }

        target.setVertexMode(primitive, irefs);
    }

    private void execAddVertex(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        float[] pos = new float[3];

        if ((opcode & 1) != 0)
        {
            pos[0] = Float.intBitsToFloat(this.unpackBits(32));
            pos[1] = Float.intBitsToFloat(this.unpackBits(32));
            pos[2] = Float.intBitsToFloat(this.unpackBits(32));
        }
        else
        {
            pos[0] = fromHalfFloat(this.unpackBits(16));
            pos[1] = fromHalfFloat(this.unpackBits(16));
            pos[2] = fromHalfFloat(this.unpackBits(16));
        }

        float[] normal = null;

        if ((opcode & 2) != 0)
        {
            normal = decompressNormalVector(this.unpackBits(18), (float[])null);
        }

        float[] color = null;

        if ((opcode & 4) != 0)
        {
            color = toFloatColor(this.unpackBits(32), (float[])null);
        }

        float[] texcoords = null;

        if ((opcode & 16) != 0)
        {
            texcoords = new float[2];

            if ((opcode & 8) != 0)
            {
                texcoords[0] = Float.intBitsToFloat(this.unpackBits(32));
                texcoords[1] = Float.intBitsToFloat(this.unpackBits(32));
            }
            else
            {
                texcoords[0] = fromHalfFloat(this.unpackBits(16));
                texcoords[1] = fromHalfFloat(this.unpackBits(16));
            }
        }

        target.addVertex(pos, normal, color, texcoords);
    }

    private void execAddVertexRef(int opcode, IGeometryBuilder target) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            target.addVertexRef(this.unpackBits(32));
        }
        else
        {
            target.addVertexRef(this.unpackBits(8));
        }
    }

    private void execNOP(int opcode) throws GeometryStreamException
    {
        if ((opcode & 1) != 0)
        {
            this._readPos = this._readPos + 7 & -8;
        }
    }

    public int executeStream(IGeometryBuilder target) throws GeometryStreamException
    {
        if (target == null)
        {
            return 0;
        }
        else
        {
            int originalReadPos = this._readPos;
            int opCount = 0;

            while (this._readPos < this._endPos)
            {
                int opcode = this.unpackBits(6);
                ++opCount;

                if ((opcode & 32) != 0)
                {
                    if ((opcode & 24) != 0)
                    {
                        this.execAddVertex(opcode, target);
                    }
                    else if ((opcode & 4) != 0)
                    {
                        this.execSetTexture(opcode, target);
                    }
                    else
                    {
                        this.execSetTextureCoordinates(opcode, target);
                    }
                }
                else if ((opcode & 16) != 0)
                {
                    this.execSetVertexMode(opcode, target);
                }
                else if ((opcode & 8) != 0)
                {
                    if ((opcode & 4) != 0)
                    {
                        if ((opcode & 2) != 0)
                        {
                            this.execSetPositionTransform(opcode, target);
                        }
                        else
                        {
                            this.execSetNormal(opcode, target);
                        }
                    }
                    else if ((opcode & 2) != 0)
                    {
                        this.execSetColor(opcode, target);
                    }
                    else
                    {
                        this.execSetTextureTransform(opcode, target);
                    }
                }
                else if ((opcode & 4) != 0)
                {
                    if ((opcode & 2) == 0)
                    {
                        throw new GeometryStreamException("Invalid opcode (" + opcode + ") found in stream.");
                    }

                    this.execAddVertexRef(opcode, target);
                }
                else
                {
                    if ((opcode & 2) == 0)
                    {
                        throw new GeometryStreamException("Invalid opcode (" + opcode + ") found in stream.");
                    }

                    this.execNOP(opcode);
                    --opCount;
                }
            }

            this._readPos = originalReadPos;
            return opCount;
        }
    }

    public int getStreamDataSize()
    {
        return (this._endPos + 32 + 13) / 8;
    }

    private void getRawStreamData(OutputStream output) throws IOException
    {
        if (output != null)
        {
            int startingBitCount = this._endPos;
            this.packBits(3, 6);
            output.write(this._stream, 0, (this._endPos + 7) / 8);
            this._endPos = startingBitCount;
        }
    }

    public int getStreamData(OutputStream output) throws IOException
    {
        if (output == null)
        {
            return 0;
        }
        else
        {
            int streamSize = (this._endPos + 13) / 8;
            output.write((byte)streamSize);
            output.write((byte)(streamSize >> 8));
            output.write((byte)(streamSize >> 16));
            output.write((byte)(streamSize >> 24));
            this.getRawStreamData(output);
            return streamSize + 4;
        }
    }

    public static int getStreamData(Collection<GeometryStream> streams, OutputStream output) throws IOException
    {
        if (output == null)
        {
            return 0;
        }
        else
        {
            int size = 0;
            
            for (GeometryStream stream : streams) {
            	if (stream != null)
                {
                    size += (stream._endPos + 13) / 8;
                }
            }

            output.write((byte)size);
            output.write((byte)(size >> 8));
            output.write((byte)(size >> 16));
            output.write((byte)(size >> 24));
            
            for (GeometryStream stream : streams) {            	
                if (stream != null)
                {
                    stream.getRawStreamData(output);
                }
            }

            return size + 4;
        }
    }

    public void forceFullPrecisionPosCoords(boolean force)
    {
        if (force)
        {
            this._flags |= 1;
        }
        else
        {
            this._flags &= -2;
        }
    }

    public void forceFullPrecisionTexCoords(boolean force)
    {
        if (force)
        {
            this._flags |= 2;
        }
        else
        {
            this._flags &= -3;
        }
    }

    private boolean useFullPosPrecision(float[] pos)
    {
        return (this._flags & 1) != 0;
    }

    private boolean useFullTexPrecision(float[] texcoords)
    {
        return (this._flags & 2) != 0;
    }

    private void packBits(int data, int bits)
    {
        if (bits > 0)
        {
            if (bits > 32)
            {
                bits = 32;
            }

            if (this._stream == null)
            {
                this._stream = new byte[192];
                this._endPos = 0;
            }
            else if ((this._endPos + bits + 7) / 8 > this._stream.length)
            {
                this._stream = Arrays.copyOf(this._stream, this._stream.length * 2);
            }

            int d = bits == 32 ? data : data & ~(-1 << bits);
            int idx = this._endPos / 8;
            int rem = this._endPos % 8;

            for (this._endPos += bits; bits > 0; ++idx)
            {
                int b = Math.min(bits, 8 - rem);
                this._stream[idx] = (byte)(this._stream[idx] | d << rem);
                bits -= b;
                d >>>= b;
                rem = 0;
            }
        }
    }

    private int unpackBits(int bits) throws GeometryStreamException
    {
        if (bits <= 0)
        {
            return 0;
        }
        else
        {
            if (bits > 32)
            {
                bits = 32;
            }

            if (this._readPos <= 0)
            {
                this._readPos = 0;
            }

            if (this._readPos + bits > this._endPos)
            {
                throw new GeometryStreamException("Unexpected end of stream.");
            }
            else
            {
                int data = 0;
                int idx = this._readPos / 8;
                int rem = this._readPos % 8;
                this._readPos += bits;

                for (int read = 0; bits > 0; ++idx)
                {
                    int b = Math.min(bits, 8 - rem);
                    bits -= b;
                    data |= (this._stream[idx] >>> rem & ~(-1 << b)) << read;
                    read += b;
                    rem = 0;
                }

                return data;
            }
        }
    }

    private static int to32BitColor(float r, float g, float b, float a)
    {
        if (r < 0.0F)
        {
            r = 0.0F;
        }
        else if (r > 1.0F)
        {
            r = 1.0F;
        }

        if (g < 0.0F)
        {
            g = 0.0F;
        }
        else if (g > 1.0F)
        {
            g = 1.0F;
        }

        if (b < 0.0F)
        {
            b = 0.0F;
        }
        else if (b > 1.0F)
        {
            b = 1.0F;
        }

        if (a < 0.0F)
        {
            a = 0.0F;
        }
        else if (a > 1.0F)
        {
            a = 1.0F;
        }

        int color = (int)(r * 255.0F) << 24;
        color |= (int)(g * 255.0F) << 16;
        color |= (int)(b * 255.0F) << 8;
        color |= (int)(a * 255.0F);
        return color;
    }

    private static float[] toFloatColor(int color, float[] output)
    {
        if (output == null)
        {
            output = new float[4];
        }

        output[0] = (float)(color >>> 24 & 255) / 255.0F;
        output[1] = (float)(color >>> 16 & 255) / 255.0F;
        output[2] = (float)(color >>> 8 & 255) / 255.0F;
        output[3] = (float)(color & 255) / 255.0F;
        return output;
    }

    private static int toHalfFloat(float fval)
    {
        int fbits = Float.floatToIntBits(fval);
        int sign = fbits >>> 16 & 32768;
        int val = (fbits & Integer.MAX_VALUE) + 4096;

        if (val >= 1199570944)
        {
            return (fbits & Integer.MAX_VALUE) >= 1199570944 ? (val < 2139095040 ? sign | 31744 : sign | 31744 | (fbits & 8388607) >>> 13) : sign | 31743;
        }
        else if (val >= 947912704)
        {
            return sign | val - 939524096 >>> 13;
        }
        else if (val < 855638016)
        {
            return sign;
        }
        else
        {
            val = (fbits & Integer.MAX_VALUE) >>> 23;
            return sign | (fbits & 8388607 | 8388608) + (8388608 >>> val - 102) >>> 126 - val;
        }
    }

    private static float fromHalfFloat(int halfFloat)
    {
        int mant = halfFloat & 1023;
        int exp = halfFloat & 31744;

        if (exp == 31744)
        {
            exp = 261120;
        }
        else if (exp != 0)
        {
            exp += 114688;

            if (mant == 0 && exp > 115712)
            {
                return Float.intBitsToFloat((halfFloat & 32768) << 16 | exp << 13 | 1023);
            }
        }
        else if (mant != 0)
        {
            exp = 115712;

            do
            {
                mant <<= 1;
                exp -= 1024;
            }
            while ((mant & 1024) == 0);

            mant &= 1023;
        }

        return Float.intBitsToFloat((halfFloat & 32768) << 16 | (exp | mant) << 13);
    }

    private static int compressNormalVector(float x, float y, float z)
    {
        if (x == 0.0F && y == 0.0F && z == 0.0F)
        {
            throw new IllegalArgumentException("Zero-length normal vector");
        }
        else
        {
            int octant = 0;

            if (x < 0.0F)
            {
                x = -x;
                octant |= 1;
            }

            if (y < 0.0F)
            {
                y = -y;
                octant |= 2;
            }

            if (z < 0.0F)
            {
                z = -z;
                octant |= 4;
            }

            int sextant = 0;
            float phi;

            if (x < z)
            {
                phi = x;
                x = z;
                z = phi;
                sextant |= 4;
            }

            if (y < z)
            {
                phi = y;
                y = z;
                z = phi;
                sextant |= 2;
            }

            if (x < y)
            {
                phi = x;
                x = y;
                y = phi;
                sextant |= 1;
            }

            double phi1 = Math.atan2((double)y, (double)x);
            double astTheta = Math.asin((double)z / Math.sqrt((double)(x * x + y * y)));
            double nphi = 63.0D * phi1 * 4.0D / Math.PI;
            double ntheta = 63.0D * astTheta * 4.0D / Math.PI;
            int np = (int)(nphi + 0.5D);
            int nt = (int)(ntheta + 0.5D);
            return octant << 15 | sextant << 12 | np << 6 | nt;
        }
    }

    private static float[] decompressNormalVector(int compressedVector, float[] output) throws GeometryStreamException
    {
        int nt = compressedVector & 63;
        int np = compressedVector >>> 6 & 63;
        int sextant = compressedVector >>> 12 & 7;
        int octant = compressedVector >>> 15 & 7;

        if (nt > np)
        {
            throw new GeometryStreamException("Invalid or corrupt compressed vector (" + compressedVector + ")");
        }
        else
        {
            int idx = (np * (np + 1) / 2 + nt) * 3;
            float x = _normalVectorTable[idx + 0];
            float y = _normalVectorTable[idx + 1];
            float z = _normalVectorTable[idx + 2];

            if ((sextant & 3) == 3)
            {
                throw new GeometryStreamException("Invalid or corrupt compressed vector (" + compressedVector + ")");
            }
            else
            {
                float tmp;

                if ((sextant & 1) != 0)
                {
                    tmp = x;
                    x = y;
                    y = tmp;
                }

                if ((sextant & 2) != 0)
                {
                    tmp = y;
                    y = z;
                    z = tmp;
                }

                if ((sextant & 4) != 0)
                {
                    tmp = x;
                    x = z;
                    z = tmp;
                }

                if ((octant & 4) != 0)
                {
                    z = -z;
                }

                if ((octant & 2) != 0)
                {
                    y = -y;
                }

                if ((octant & 1) != 0)
                {
                    x = -x;
                }

                if (output == null)
                {
                    output = new float[] {x, y, z};
                }
                else
                {
                    output[0] = x;
                    output[1] = y;
                    output[2] = z;
                }

                return output;
            }
        }
    }

    static
    {
        for (int np = 0; np < 64; ++np)
        {
            for (int nt = 0; nt <= np; ++nt)
            {
                int idx = (np * (np + 1) / 2 + nt) * 3;
                double phi = (Math.PI / 4D) * ((double)np / 63.0D);
                double theta = Math.atan(Math.sin((Math.PI / 4D) * ((double)nt / 63.0D)));
                double x = Math.cos(theta) * Math.cos(phi);
                double y = Math.cos(theta) * Math.sin(phi);
                double z = Math.sin(theta);
                _normalVectorTable[idx + 0] = (float)x;
                _normalVectorTable[idx + 1] = (float)y;
                _normalVectorTable[idx + 2] = (float)z;
            }
        }
    }
    
    public static class GeometryStreamException extends Exception
    {
        public GeometryStreamException(String message, Throwable cause)
        {
            super(message, cause);
        }

        public GeometryStreamException(String message)
        {
            super(message);
        }

        public GeometryStreamException(Throwable cause)
        {
            super(cause);
        }
    }

}
