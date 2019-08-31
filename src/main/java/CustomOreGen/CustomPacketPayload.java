package CustomOreGen;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

import net.minecraft.network.PacketBuffer;

public class CustomPacketPayload
{
    public final PayloadType type;
    public final Serializable data;    
    
    public CustomPacketPayload(PayloadType type, Serializable data)
    {
        this.type = type;
        this.data = data;
    }

    public static void encode(CustomPacketPayload msg, PacketBuffer buf)
    {
        buf.writeByte((byte)msg.type.ordinal());
        buf.writeBytes(SerializationUtils.serialize(msg.data));
    }
    
    public static CustomPacketPayload decode(PacketBuffer buf)
    {
    	PayloadType type1 = PayloadType.values()[buf.readByte()];
    	Serializable data1 = (Serializable)SerializationUtils.deserialize(buf.readByteArray());
    	return new CustomPacketPayload(type1, data1);
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
}
