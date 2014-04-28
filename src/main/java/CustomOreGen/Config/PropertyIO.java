package CustomOreGen.Config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PropertyIO
{
    private static final char[] hexDigit = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static void save(Map properties, OutputStream out, String headerComments) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
        boolean escUnicode = true;

        if (headerComments != null)
        {
            writeComments(bw, headerComments);
        }

        bw.write("#" + (new Date()).toString());
        bw.newLine();
        Iterator i$ = properties.entrySet().iterator();

        while (i$.hasNext())
        {
            Entry property = (Entry)i$.next();
            String key = saveConvert((String)property.getKey(), true, escUnicode);
            String val = saveConvert((String)property.getValue(), false, escUnicode);
            bw.write(key + "=" + val);
            bw.newLine();
        }

        bw.flush();
    }

    private static void writeComments(BufferedWriter bw, String comments) throws IOException
    {
        bw.write("#");
        int len = comments.length();
        int current = 0;
        int last = 0;

        for (char[] uu = new char[] {'\\', 'u', '\u0000', '\u0000', '\u0000', '\u0000'}; current < len; ++current)
        {
            char c = comments.charAt(current);

            if (c > 255 || c == 10 || c == 13)
            {
                if (last != current)
                {
                    bw.write(comments.substring(last, current));
                }

                if (c > 255)
                {
                    uu[2] = hexDigit[c >> 12 & 15];
                    uu[3] = hexDigit[c >> 8 & 15];
                    uu[4] = hexDigit[c >> 4 & 15];
                    uu[5] = hexDigit[c & 15];
                    bw.write(new String(uu));
                }
                else
                {
                    bw.newLine();

                    if (c == 13 && current != len - 1 && comments.charAt(current + 1) == 10)
                    {
                        ++current;
                    }

                    if (current == len - 1 || comments.charAt(current + 1) != 35 && comments.charAt(current + 1) != 33)
                    {
                        bw.write("#");
                    }
                }

                last = current + 1;
            }
        }

        if (last != current)
        {
            bw.write(comments.substring(last, current));
        }

        bw.newLine();
    }

    private static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode)
    {
        int len = theString.length();
        int bufLen = len * 2;

        if (bufLen < 0)
        {
            bufLen = Integer.MAX_VALUE;
        }

        StringBuffer outBuffer = new StringBuffer(bufLen);

        for (int x = 0; x < len; ++x)
        {
            char aChar = theString.charAt(x);

            if (aChar > 61 && aChar < 127)
            {
                if (aChar == 92)
                {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                }
                else
                {
                    outBuffer.append(aChar);
                }
            }
            else
            {
                switch (aChar)
                {
                    case 9:
                        outBuffer.append('\\');
                        outBuffer.append('t');
                        break;

                    case 10:
                        outBuffer.append('\\');
                        outBuffer.append('n');
                        break;

                    case 12:
                        outBuffer.append('\\');
                        outBuffer.append('f');
                        break;

                    case 13:
                        outBuffer.append('\\');
                        outBuffer.append('r');
                        break;

                    case 32:
                        if (x == 0 || escapeSpace)
                        {
                            outBuffer.append('\\');
                        }

                        outBuffer.append(' ');
                        break;

                    case 33:
                    case 35:
                    case 58:
                    case 61:
                        outBuffer.append('\\');
                        outBuffer.append(aChar);
                        break;

                    default:
                        if ((aChar < 32 || aChar > 126) & escapeUnicode)
                        {
                            outBuffer.append('\\');
                            outBuffer.append('u');
                            outBuffer.append(hexDigit[aChar >> 12 & 15]);
                            outBuffer.append(hexDigit[aChar >> 8 & 15]);
                            outBuffer.append(hexDigit[aChar >> 4 & 15]);
                            outBuffer.append(hexDigit[aChar & 15]);
                        }
                        else
                        {
                            outBuffer.append(aChar);
                        }
                }
            }
        }

        return outBuffer.toString();
    }

    public static void load(Map properties, InputStream inStream) throws IOException
    {
        LineReader lr = new LineReader(inStream);
        char[] convtBuf = new char[1024];
        int limit;

        while ((limit = lr.readLine()) >= 0)
        {
            boolean c = false;
            int keyLen = 0;
            int valueStart = limit;
            boolean hasSep = false;
            boolean precedingBackslash = false;

            while (true)
            {
                char var12;

                if (keyLen < limit)
                {
                    var12 = lr.lineBuf[keyLen];

                    if ((var12 == 61 || var12 == 58) && !precedingBackslash)
                    {
                        valueStart = keyLen + 1;
                        hasSep = true;
                    }
                    else
                    {
                        if (var12 != 32 && var12 != 9 && var12 != 12 || precedingBackslash)
                        {
                            if (var12 == 92)
                            {
                                precedingBackslash = !precedingBackslash;
                            }
                            else
                            {
                                precedingBackslash = false;
                            }

                            ++keyLen;
                            continue;
                        }

                        valueStart = keyLen + 1;
                    }
                }

                for (; valueStart < limit; ++valueStart)
                {
                    var12 = lr.lineBuf[valueStart];

                    if (var12 != 32 && var12 != 9 && var12 != 12)
                    {
                        if (hasSep || var12 != 61 && var12 != 58)
                        {
                            break;
                        }

                        hasSep = true;
                    }
                }

                String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                properties.put(key, value);
                break;
            }
        }
    }

    private static String loadConvert(char[] in, int off, int len, char[] convtBuf)
    {
        if (convtBuf.length < len)
        {
            int aChar = len * 2;

            if (aChar < 0)
            {
                aChar = Integer.MAX_VALUE;
            }

            convtBuf = new char[aChar];
        }

        char[] out = convtBuf;
        int outLen = 0;
        int end = off + len;

        while (off < end)
        {
            char var10 = in[off++];

            if (var10 == 92)
            {
                var10 = in[off++];

                if (var10 == 117)
                {
                    int value = 0;

                    for (int i = 0; i < 4; ++i)
                    {
                        var10 = in[off++];

                        switch (var10)
                        {
                            case 48:
                            case 49:
                            case 50:
                            case 51:
                            case 52:
                            case 53:
                            case 54:
                            case 55:
                            case 56:
                            case 57:
                                value = (value << 4) + var10 - 48;
                                break;

                            case 58:
                            case 59:
                            case 60:
                            case 61:
                            case 62:
                            case 63:
                            case 64:
                            case 71:
                            case 72:
                            case 73:
                            case 74:
                            case 75:
                            case 76:
                            case 77:
                            case 78:
                            case 79:
                            case 80:
                            case 81:
                            case 82:
                            case 83:
                            case 84:
                            case 85:
                            case 86:
                            case 87:
                            case 88:
                            case 89:
                            case 90:
                            case 91:
                            case 92:
                            case 93:
                            case 94:
                            case 95:
                            case 96:
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");

                            case 65:
                            case 66:
                            case 67:
                            case 68:
                            case 69:
                            case 70:
                                value = (value << 4) + 10 + var10 - 65;
                                break;

                            case 97:
                            case 98:
                            case 99:
                            case 100:
                            case 101:
                            case 102:
                                value = (value << 4) + 10 + var10 - 97;
                        }
                    }

                    out[outLen++] = (char)value;
                }
                else
                {
                    if (var10 == 116)
                    {
                        var10 = 9;
                    }
                    else if (var10 == 114)
                    {
                        var10 = 13;
                    }
                    else if (var10 == 110)
                    {
                        var10 = 10;
                    }
                    else if (var10 == 102)
                    {
                        var10 = 12;
                    }

                    out[outLen++] = var10;
                }
            }
            else
            {
                out[outLen++] = var10;
            }
        }

        return new String(out, 0, outLen);
    }
    
    private static class LineReader
    {
        byte[] inByteBuf;
        char[] inCharBuf;
        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;
        InputStream inStream;
        Reader reader;

        public LineReader(InputStream inStream)
        {
            this.inStream = inStream;
            this.inByteBuf = new byte[8192];
        }

        int readLine() throws IOException
        {
            int len = 0;
            boolean c = false;
            boolean skipWhiteSpace = true;
            boolean isCommentLine = false;
            boolean isNewLine = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
            boolean skipLF = false;

            while (true)
            {
                if (this.inOff >= this.inLimit)
                {
                    this.inLimit = this.inStream == null ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                    this.inOff = 0;

                    if (this.inLimit <= 0)
                    {
                        if (len != 0 && !isCommentLine)
                        {
                            return len;
                        }

                        return -1;
                    }
                }

                char var11;

                if (this.inStream != null)
                {
                    var11 = (char)(255 & this.inByteBuf[this.inOff++]);
                }
                else
                {
                    var11 = this.inCharBuf[this.inOff++];
                }

                if (skipLF)
                {
                    skipLF = false;

                    if (var11 == 10)
                    {
                        continue;
                    }
                }

                if (skipWhiteSpace)
                {
                    if (var11 == 32 || var11 == 9 || var11 == 12 || !appendedLineBegin && (var11 == 13 || var11 == 10))
                    {
                        continue;
                    }

                    skipWhiteSpace = false;
                    appendedLineBegin = false;
                }

                if (isNewLine)
                {
                    isNewLine = false;

                    if (var11 == 35 || var11 == 33)
                    {
                        isCommentLine = true;
                        continue;
                    }
                }

                if (var11 != 10 && var11 != 13)
                {
                    this.lineBuf[len++] = var11;

                    if (len == this.lineBuf.length)
                    {
                        int newLength = this.lineBuf.length * 2;

                        if (newLength < 0)
                        {
                            newLength = Integer.MAX_VALUE;
                        }

                        char[] buf = new char[newLength];
                        System.arraycopy(this.lineBuf, 0, buf, 0, this.lineBuf.length);
                        this.lineBuf = buf;
                    }

                    if (var11 == 92)
                    {
                        precedingBackslash = !precedingBackslash;
                    }
                    else
                    {
                        precedingBackslash = false;
                    }
                }
                else if (!isCommentLine && len != 0)
                {
                    if (this.inOff >= this.inLimit)
                    {
                        this.inLimit = this.inStream == null ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                        this.inOff = 0;

                        if (this.inLimit <= 0)
                        {
                            return len;
                        }
                    }

                    if (!precedingBackslash)
                    {
                        return len;
                    }

                    --len;
                    skipWhiteSpace = true;
                    appendedLineBegin = true;
                    precedingBackslash = false;

                    if (var11 == 13)
                    {
                        skipLF = true;
                    }
                }
                else
                {
                    isCommentLine = false;
                    isNewLine = true;
                    skipWhiteSpace = true;
                    len = 0;
                }
            }
        }
    }

}
