package CustomOreGen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.BaseMod;
import net.minecraft.src.ModLoader;

public class CustomPacketPayload
{
    public final PayloadType type;
    public final Serializable data;
    private static Map _xpacketMap = new HashMap();
    private static AtomicInteger _xpacketNextID = new AtomicInteger(0);
    private static final String CHANNEL = "CustomOreGen";
    private static final String XCHANNEL = "CustomOreGenX";
    private static final int MAX_SIZE = 32000;

    public CustomPacketPayload(PayloadType type, Serializable data)
    {
        this.type = type;
        this.data = data;
    }

    public void sendToServer()
    {
    	for (Packet250CustomPayload packet : this.createPackets()) {
    		ModLoader.sendPacket(packet);
        }
    }

    public void sendToClient(NetServerHandler handler)
    {
    	for (Packet250CustomPayload packet : this.createPackets()) {
    		ModLoader.serverSendPacket(handler, packet);
        }
    }

    public void sendToAllClients()
    {
    	for (Packet250CustomPayload packet : this.createPackets()) {
    		MinecraftServer.getServer().getConfigurationManager().sendPacketToAllPlayers(packet);
        }
    }

    private Packet250CustomPayload[] createPackets()
    {
        Object payloadData = null;
        boolean compressed = false;
        byte[] var11;

        try
        {
            AutoCompressionStream packetCount = new AutoCompressionStream(1024);
            ObjectOutputStream packets = new ObjectOutputStream(packetCount);
            packets.writeByte((byte)this.type.ordinal());
            packets.writeObject(this.data);
            packets.close();
            packetCount.close();
            var11 = packetCount.toByteArray();
            compressed = packetCount.isCompressed();
        }
        catch (IOException var10)
        {
            throw new RuntimeException(var10);
        }

        if (!compressed)
        {
            return new Packet250CustomPayload[] {new Packet250CustomPayload("CustomOreGen", var11)};
        }
        else
        {
            int var12 = (var11.length + 32000 - 1) / 32000;
            Packet250CustomPayload[] var13 = new Packet250CustomPayload[var12];
            int id = _xpacketNextID.incrementAndGet();
            int i = 1;

            for (int offset = 0; i <= var12; ++i)
            {
                int dataLen = Math.min(32000, var11.length - offset);
                byte[] piece = new byte[8 + dataLen];
                piece[0] = (byte)id;
                piece[1] = (byte)(id >> 8);
                piece[2] = (byte)(id >> 16);
                piece[3] = (byte)(id >> 24);
                piece[4] = (byte)var12;
                piece[5] = (byte)(var12 >> 8);
                piece[6] = (byte)i;
                piece[7] = (byte)(i >> 8);
                System.arraycopy(var11, offset, piece, 8, dataLen);
                offset += dataLen;
                var13[i - 1] = new Packet250CustomPayload("CustomOreGenX", piece);
            }

            return var13;
        }
    }

    public static CustomPacketPayload decodePacket(Packet250CustomPayload packet)
    {
        try
        {
            Object ex = null;

            if (packet.channel.equals("CustomOreGenX"))
            {
                int objStream = packet.data[0] & 255;
                objStream |= (packet.data[1] & 255) << 8;
                objStream |= (packet.data[2] & 255) << 16;
                objStream |= (packet.data[3] & 255) << 24;
                int type = packet.data[4] & 255;
                type |= (packet.data[5] & 255) << 8;
                int data = packet.data[6] & 255;
                data |= (packet.data[7] & 255) << 8;

                if (type > 1)
                {
                    Map var5 = _xpacketMap;

                    synchronized (_xpacketMap)
                    {
                        ByteArrayOutputStream partialData = (ByteArrayOutputStream)_xpacketMap.get(Integer.valueOf(objStream));

                        if (partialData == null)
                        {
                            partialData = new ByteArrayOutputStream(32000 * (type + 1));
                            _xpacketMap.put(Integer.valueOf(objStream), partialData);
                        }

                        if (partialData.size() != (data - 1) * 32000)
                        {
                            throw new RuntimeException("Packet # " + data + "/" + type + " in group " + objStream + " does not match next position in buffer " + (partialData.size() / 32000 + 1));
                        }

                        partialData.write(packet.data, 8, packet.data.length - 8);

                        if (data < type)
                        {
                            return null;
                        }

                        _xpacketMap.remove(Integer.valueOf(objStream));
                        partialData.close();
                        ex = new InflaterInputStream(new ByteArrayInputStream(partialData.toByteArray()));
                    }
                }
                else
                {
                    ex = new InflaterInputStream(new ByteArrayInputStream(packet.data, 8, packet.data.length - 8));
                }
            }
            else
            {
                if (!packet.channel.equals("CustomOreGen"))
                {
                    CustomOreGenBase.log.warning("Invalid custom packet channel: \'" + packet.channel + "\'");
                    return null;
                }

                ex = new ByteArrayInputStream(packet.data);
            }

            TranslatingObjectInputStream objStream1 = new TranslatingObjectInputStream((InputStream)ex);
            PayloadType type1 = PayloadType.values()[objStream1.readByte()];
            Serializable data1 = (Serializable)objStream1.readObject();
            return new CustomPacketPayload(type1, data1);
        }
        catch (Exception var9)
        {
            CustomOreGenBase.log.warning("Error while decoding custom packet payload: " + var9.getMessage());
            return null;
        }
    }

    public static void registerChannels(BaseMod mod)
    {
        ModLoader.registerPacketChannel(mod, "CustomOreGen");
        ModLoader.registerPacketChannel(mod, "CustomOreGenX");
    }
    
    private class AutoCompressionStream extends OutputStream
    {
        private int compressionThreshold;
        private ByteArrayOutputStream backingStream;
        private DeflaterOutputStream compressionStream;

        public AutoCompressionStream(int threshold)
        {
            this.compressionThreshold = threshold;
            this.backingStream = new ByteArrayOutputStream();
            this.compressionStream = null;
        }

        public void write(int b) throws IOException
        {
            if (this.compressionStream != null)
            {
                this.compressionStream.write(b);
            }
            else if (this.backingStream.size() < this.compressionThreshold)
            {
                this.backingStream.write(b);
            }
            else
            {
                byte[] data = this.backingStream.toByteArray();
                this.backingStream.reset();
                this.compressionStream = new DeflaterOutputStream(this.backingStream, new Deflater(9));
                this.compressionStream.write(data);
                this.compressionStream.write(b);
            }
        }

        public void close() throws IOException
        {
            if (this.compressionStream != null)
            {
                this.compressionStream.close();
            }

            this.backingStream.close();
        }

        public void flush() throws IOException
        {
            if (this.compressionStream != null)
            {
                this.compressionStream.flush();
            }

            this.backingStream.flush();
        }

        public boolean isCompressed()
        {
            return this.compressionStream != null;
        }

        public byte[] toByteArray() throws IOException
        {
            this.flush();
            return this.backingStream.toByteArray();
        }
    }
    
    public enum PayloadType
    {
        DebuggingGeometryRequest,
        DebuggingGeometryReset,
        DebuggingGeometryData,
        DebuggingGeometryRenderMode,
        MystcraftSymbolData,
        CommandResponse;
    }
    
    private static class TranslatingObjectInputStream extends ObjectInputStream
    {
        public TranslatingObjectInputStream(InputStream in) throws IOException
        {
            super(in);
        }

        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
        {
            try
            {
                return super.resolveClass(desc);
            }
            catch (ClassNotFoundException var3)
            {
                return desc.getName().startsWith("net.minecraft.src.") ? CustomOreGenBase.class.getClassLoader().loadClass(desc.getName().substring(18)) : CustomOreGenBase.class.getClassLoader().loadClass("net.minecraft.src." + desc.getName());
            }
        }
    }



}
