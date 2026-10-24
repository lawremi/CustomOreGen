package CustomOreGen;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Server.ServerState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {

	}

	// 2: payloads are encoded field by field instead of with Java serialization.
	private static final int PROTOCOL_VERSION = 2;
	private static final String CHANNEL_NAME = "customoregen";

	private static final SimpleChannel CHANNEL = buildChannel(CHANNEL_NAME);

	private static final SimpleChannel buildChannel(String name) {
		return ChannelBuilder
				.named(ResourceLocation.fromNamespaceAndPath(CustomOreGenBase.MODID, name))
				.networkProtocolVersion(PROTOCOL_VERSION)
				.simpleChannel();
	}

	public static void register()
	{
		CHANNEL.messageBuilder(CustomPacketPayload.class, 0)
				.encoder(CustomPacketPayload::encode)
				.decoder(CustomPacketPayload::decode)
				.consumerMainThread(CustomPacketPayloadHandler::handle)
				.add();
	}

	// Registered with consumerMainThread, so this already runs on the main thread.
	private static void handle(CustomPacketPayload msg, CustomPayloadEvent.Context context) {
		if (context.isClientSide()) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CustomOreGen.Client.ClientPacketHandler.handle(msg));
		} else {
			handleServer(msg, context.getSender());
		}
		context.setPacketHandled(true);
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
	public static void sendToServer(Object msg)
	{
		CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
	}

	/**
	 * Send a packet to a specific player.<br>
	 * Must be called Server side.
	 */
	public static void sendTo(Object msg, ServerPlayer player)
	{
		// Forge no longer has FakePlayer; fake players are the ones without a real connection.
		if (player.connection != null && player.connection.getConnection() != null)
		{
			CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
		}
	}

	public static void sendToAllClients(Object msg) {
		CHANNEL.send(msg, PacketDistributor.ALL.noArg());
	}
}
