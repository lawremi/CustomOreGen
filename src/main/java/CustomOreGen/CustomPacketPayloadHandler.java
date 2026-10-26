package CustomOreGen;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Server.ServerState;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {

	}

	// 2: payloads are encoded field by field instead of with Java serialization.
	private static final String PROTOCOL_VERSION = "2";

	/** COG's payload as a network packet (it encodes itself, as it did on Forge's channel). */
	public record Packet(CustomPacketPayload msg) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<Packet> TYPE = new Type<Packet>(ResourceLocation.fromNamespaceAndPath(CustomOreGenBase.MODID, "customoregen"));
		public static final StreamCodec<FriendlyByteBuf, Packet> STREAM_CODEC = StreamCodec.of(
				(buf, packet) -> CustomPacketPayload.encode(packet.msg(), buf),
				buf -> new Packet(CustomPacketPayload.decode(buf)));

		@Override
		public Type<Packet> type() {
			return TYPE;
		}
	}

	public static void register(RegisterPayloadHandlersEvent event)
	{
		// Handlers run on the main thread.
		event.registrar(PROTOCOL_VERSION).playBidirectional(Packet.TYPE, Packet.STREAM_CODEC, CustomPacketPayloadHandler::handle);
	}

	private static void handle(Packet packet, IPayloadContext context) {
		if (context.flow().isClientbound()) {
			CustomOreGen.Client.ClientPacketHandler.handle(packet.msg());
		} else {
			handleServer(packet.msg(), (ServerPlayer)context.player());
		}
	}

	private static void handleServer(CustomPacketPayload msg, ServerPlayer player)
    {
		if (player == null || msg == null)
		{
			return;
		}
		MinecraftServer server = player.getServer();
        ServerState.checkIfServerChanged(server, server.getWorldData());

        switch (msg.type)
        {
            case DebuggingGeometryRequest:
            	// Wireframes reveal where ore is, so only operators (or the single-player host) get them.
            	if (!server.getPlayerList().isOp(player.getGameProfile()) && !server.isSingleplayerOwner(player.getGameProfile()))
            	{
            		return;
            	}
                GeometryRequestData request = (GeometryRequestData)msg.data;
                ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, request.dimensionID));
                if (level == null)
                {
                	return;
                }
                request.world = level;
                GeometryData geometryData = ServerState.getDebuggingGeometryData(request);

                // Null means debugging mode is off for that dimension; the client is told to start over
                // (DebuggingGeometryReset) when it gets turned on.
                if (geometryData != null)
                {
                    sendTo(new CustomPacketPayload(PayloadType.DebuggingGeometryData, geometryData), player);
                }
                break;

            default:
                CustomOreGenBase.log.warn("Ignoring unexpected packet type " + msg.type + " from " + player.getName().getString());
        }
    }

	/**
	 * Sends a packet to the server.<br>
	 * Must be called Client side.
	 */
	public static void sendToServer(CustomPacketPayload msg)
	{
		PacketDistributor.sendToServer(new Packet(msg));
	}

	/**
	 * Send a packet to a specific player.<br>
	 * Must be called Server side.
	 */
	public static void sendTo(CustomPacketPayload msg, ServerPlayer player)
	{
		// Fake players are the ones without a real connection.
		if (player.connection != null && player.connection.getConnection() != null)
		{
			PacketDistributor.sendToPlayer(player, new Packet(msg));
		}
	}

	public static void sendToAllClients(CustomPacketPayload msg) {
		PacketDistributor.sendToAllPlayers(new Packet(msg));
	}
}
