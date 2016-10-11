package CustomOreGen.Config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public class PropertyIO
{
    private static final char[] hexDigit = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static void save(Map<String,String> properties, OutputStream out, String headerComments) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
        boolean escUnicode = true;

        if (headerComments != null)
        {
            writeComments(bw, headerComments);
        }

        bw.write("#" + (new Date()).toString());
        bw.newLine();
        
        for (Entry<String,String> property : properties.entrySet())
        {
            String key = saveConvert(property.getKey(), true, escUnicode);
            String val = saveConvert(property.getValue(), false, escUnicode);
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

            if (c > 255 || c == '\n' || c == '\r')
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

                    if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n')
                    {
                        ++current;
                    }

                    if (current == len - 1 || comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!')
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

            if (aChar > '=' && aChar < 127)
            {
                if (aChar == '\\')
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
                        if ((aChar < ' ' || aChar > '~') & escapeUnicode)
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

    public static void load(Map<String,String> properties, InputStream inStream) throws IOException
    {
        LineReader lr = new LineReader(inStream);
        char[] convtBuf = new char[1024];
        int limit;

        while ((limit = lr.readLine()) >= 0)
        {
            int keyLen = 0;
            int valueStart = limit;
            boolean hasSep = false;
            boolean precedingBackslash = false;

            while (true)
            {
                char ch;

                if (keyLen < limit)
                {
                    ch = lr.lineBuf[keyLen];

                    if ((ch == '=' || ch == ':') && !precedingBackslash)
                    {
                        valueStart = keyLen + 1;
                        hasSep = true;
                    }
                    else
                    {
                        if (ch != ' ' && ch != '\t' && ch != '\f' || precedingBackslash)
                        {
                            if (ch == 92)
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
                    ch = lr.lineBuf[valueStart];

                    if (ch != ' ' && ch != '\t' && ch != '\f')
                    {
                        if (hasSep || ch != '=' && ch != ':')
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
            char ch = in[off++];

            if (ch == '\\')
            {
                ch = in[off++];

                if (ch == 'u')
                {
                    int value = 0;

                    for (int i = 0; i < 4; ++i)
                    {
                        ch = in[off++];

                        switch (ch)
                        {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + ch - '0';
                                break;

                            case ':':
                            case ';':
                            case '<':
                            case '=':
                            case '>':
                            case '?':
                            case '@':
                            case 'G':
                            case 'H':
                            case 'I':
                            case 'J':
                            case 'K':
                            case 'L':
                            case 'M':
                            case 'N':
                            case 'O':
                            case 'P':
                            case 'Q':
                            case 'R':
                            case 'S':
                            case 'T':
                            case 'U':
                            case 'V':
                            case 'W':
                            case 'X':
                            case 'Y':
                            case 'Z':
                            case '[':
                            case '\\':
                            case ']':
                            case '^':
                            case '_':
                            case '`':
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.");

                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + ch - 'A';
                                break;

                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + ch - 'a';
                        }
                    }

                    out[outLen++] = (char)value;
                }
                else
                {
                    if (ch == 't')
                    {
                        ch = '\t';
                    }
                    else if (ch == 'r')
                    {
                        ch = '\r';
                    }
                    else if (ch == 'n')
                    {
                        ch = '\n';
                    }
                    else if (ch == 'f')
                    {
                        ch = '\f';
                    }

                    out[outLen++] = ch;
                }
            }
            else
            {
                out[outLen++] = ch;
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

                char ch;

                if (this.inStream != null)
                {
                    ch = (char)(255 & this.inByteBuf[this.inOff++]);
                }
                else
                {
                    ch = this.inCharBuf[this.inOff++];
                }

                if (skipLF)
                {
                    skipLF = false;

                    if (ch == '\n')
                    {
                        continue;
                    }
                }

                if (skipWhiteSpace)
                {
                    if (ch == ' ' || ch == '\t' || ch == '\f' || !appendedLineBegin && (ch == '\r' || ch == '\n'))
                    {
                        continue;
                    }

                    skipWhiteSpace = false;
                    appendedLineBegin = false;
                }

                if (isNewLine)
                {
                    isNewLine = false;

                    if (ch == '#' || ch == '!')
                    {
                        isCommentLine = true;
                        continue;
                    }
                }

                if (ch != '\n' && ch != '\r')
                {
                    this.lineBuf[len++] = ch;

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

                    if (ch == '\\')
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

                    if (ch == '\r')
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
