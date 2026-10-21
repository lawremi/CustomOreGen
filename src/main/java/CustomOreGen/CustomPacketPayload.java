package CustomOreGen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collections;

import CustomOreGen.Util.GeometryStream;
import net.minecraft.network.FriendlyByteBuf;

public class CustomPacketPayload
{
    public final PayloadType type;
    public final Serializable data;

    public CustomPacketPayload(PayloadType type, Serializable data)
    {
        this.type = type;
        this.data = data;
    }

    /*
     * Each payload type writes its fields explicitly. This used to be Java object serialization, which let
     * either side of a connection hand the other arbitrary objects to deserialize (a remote code
     * execution risk on servers).
     */
    public static void encode(CustomPacketPayload msg, FriendlyByteBuf buf)
    {
        buf.writeEnum(msg.type);

        switch (msg.type)
        {
            case DebuggingGeometryRequest:
                writeRequest((GeometryRequestData)msg.data, buf);
                break;

            case DebuggingGeometryData:
                GeometryData geometry = (GeometryData)msg.data;
                writeRequest(geometry, buf);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try
                {
                    GeometryStream.getStreamData(geometry.geometry, bytes);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                buf.writeByteArray(bytes.toByteArray());
                break;

            case DebuggingGeometryRenderMode:
            case CommandResponse:
                buf.writeNullable((String)msg.data, FriendlyByteBuf::writeUtf);
                break;

            case DebuggingGeometryReset:
                break;

            default:
                throw new IllegalArgumentException("Payload type " + msg.type + " cannot be sent");
        }
    }

    public static CustomPacketPayload decode(FriendlyByteBuf buf)
    {
        PayloadType type = buf.readEnum(PayloadType.class);

        switch (type)
        {
            case DebuggingGeometryRequest:
            {
                GeometryRequestData request = new GeometryRequestData();
                readRequest(request, buf);
                return new CustomPacketPayload(type, request);
            }

            case DebuggingGeometryData:
            {
                GeometryData geometry = new GeometryData();
                readRequest(geometry, buf);
                try
                {
                    geometry.geometry = Collections.singletonList(new GeometryStream(new ByteArrayInputStream(buf.readByteArray())));
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
                return new CustomPacketPayload(type, geometry);
            }

            case DebuggingGeometryRenderMode:
            case CommandResponse:
                return new CustomPacketPayload(type, buf.readNullable(FriendlyByteBuf::readUtf));

            case DebuggingGeometryReset:
                return new CustomPacketPayload(type, null);

            default:
                throw new IllegalArgumentException("Payload type " + type + " cannot be received");
        }
    }

    private static void writeRequest(GeometryRequestData request, FriendlyByteBuf buf)
    {
        buf.writeResourceLocation(request.dimensionID);
        buf.writeVarInt(request.chunkX);
        buf.writeVarInt(request.chunkZ);
        buf.writeVarInt(request.batchID);
    }

    private static void readRequest(GeometryRequestData request, FriendlyByteBuf buf)
    {
        request.dimensionID = buf.readResourceLocation();
        request.chunkX = buf.readVarInt();
        request.chunkZ = buf.readVarInt();
        request.batchID = buf.readVarInt();
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
