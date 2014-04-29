package CustomOreGen;

import static io.netty.buffer.Unpooled.wrappedBuffer;

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

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

public class CustomPacketPayload
{
    public final PayloadType type;
    public final Serializable data;
    private static Map<String, FMLEventChannel> channels = new HashMap();
    private static Map<Integer, ByteArrayOutputStream> _xpacketMap = new HashMap();
    private static AtomicInteger _xpacketNextID = new AtomicInteger(0);
    private static final String CHANNEL_NAME = "CustomOreGen";
    private static final String XCHANNEL_NAME = "CustomOreGenX";
    private static final int MAX_SIZE = 32000;
    
    
    public CustomPacketPayload(PayloadType type, Serializable data)
    {
        this.type = type;
        this.data = data;
    }

    public void sendToServer()
    {
    	for (FMLProxyPacket packet : this.createPackets()) {
    		channels.get(packet.channel()).sendToServer(packet);
        }
    }

    public void sendToClient(EntityPlayerMP player)
    {
    	for (FMLProxyPacket packet : this.createPackets()) {
    		channels.get(packet.channel()).sendTo(packet, player);
        }
    }

    public void sendToAllClients()
    {
    	for (FMLProxyPacket packet : this.createPackets()) {
    		channels.get(packet.channel()).sendToAll(packet);
        }
    }

    private FMLProxyPacket[] createPackets()
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
            return new FMLProxyPacket[] {new FMLProxyPacket(wrappedBuffer(var11), CHANNEL_NAME)};
        }
        else
        {
            int var12 = (var11.length + 32000 - 1) / 32000;
            FMLProxyPacket[] var13 = new FMLProxyPacket[var12];
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
                var13[i - 1] = new FMLProxyPacket(wrappedBuffer(piece), XCHANNEL_NAME);
            }

            return var13;
        }
    }

    public static CustomPacketPayload decodePacket(FMLProxyPacket packet)
    {
        try
        {
            Object ex = null;

            if (packet.channel().equals(XCHANNEL_NAME))
            {
            	byte[] packetData = packet.payload().array();
                int objStream = packetData[0] & 255;
                objStream |= (packetData[1] & 255) << 8;
                objStream |= (packetData[2] & 255) << 16;
                objStream |= (packetData[3] & 255) << 24;
                int type = packetData[4] & 255;
                type |= (packetData[5] & 255) << 8;
                int data = packetData[6] & 255;
                data |= (packetData[7] & 255) << 8;

                if (type > 1)
                {
                    synchronized (_xpacketMap)
                    {
                        ByteArrayOutputStream partialData = _xpacketMap.get(objStream);

                        if (partialData == null)
                        {
                            partialData = new ByteArrayOutputStream(32000 * (type + 1));
                            _xpacketMap.put(objStream, partialData);
                        }

                        if (partialData.size() != (data - 1) * 32000)
                        {
                            throw new RuntimeException("Packet # " + data + "/" + type + " in group " + objStream + " does not match next position in buffer " + (partialData.size() / 32000 + 1));
                        }

                        partialData.write(packetData, 8, packetData.length - 8);

                        if (data < type)
                        {
                            return null;
                        }

                        _xpacketMap.remove(objStream);
                        partialData.close();
                        ex = new InflaterInputStream(new ByteArrayInputStream(partialData.toByteArray()));
                    }
                }
                else
                {
                    ex = new InflaterInputStream(new ByteArrayInputStream(packetData, 8, packetData.length - 8));
                }
            }
            else
            {
                if (!packet.channel().equals(CHANNEL_NAME))
                {
                    CustomOreGenBase.log.warn("Invalid custom packet channel: \'" + packet.channel() + "\'");
                    return null;
                }

                ex = new ByteArrayInputStream(packet.payload().array());
            }

            TranslatingObjectInputStream objStream1 = new TranslatingObjectInputStream((InputStream)ex);
            PayloadType type1 = PayloadType.values()[objStream1.readByte()];
            Serializable data1 = (Serializable)objStream1.readObject();
            objStream1.close();
            return new CustomPacketPayload(type1, data1);
        }
        catch (Exception var9)
        {
            CustomOreGenBase.log.warn("Error while decoding custom packet payload: " + var9.getMessage());
            return null;
        }
    }

    public static void registerChannels(Object mod)
    {
    	registerChannel(mod, CHANNEL_NAME);
    	registerChannel(mod, XCHANNEL_NAME);
    }
    
    private static void registerChannel(Object mod, String name) {
    	FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(name);
        channels.put(name, channel);
        channel.register(mod);
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
