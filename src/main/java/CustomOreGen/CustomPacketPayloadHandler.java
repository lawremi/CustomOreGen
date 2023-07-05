package CustomOreGen;

import java.util.function.Supplier;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {
		
	}
	
	private static final String PROTOCOL_VERSION = Integer.toString(1);
	private static final String CHANNEL_NAME = "customoregen";
    
	private static final SimpleChannel CHANNEL = buildChannel(CHANNEL_NAME);
	
	private static final SimpleChannel buildChannel(String name) {
		return NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation(CustomOreGenBase.MODID, name))
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.simpleChannel();
	}
	
	public static void register()
	{
		CHANNEL.registerMessage(0, CustomPacketPayload.class, CustomPacketPayload::encode, CustomPacketPayload::decode, CustomPacketPayloadHandler::handle);
	}
	
	private static void handle(CustomPacketPayload msg, Supplier<NetworkEvent.Context> ctx) {
		if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
			handleClient(msg, ctx);
		} else {
			handleServer(msg, ctx);
		}
	}
	
	private static void handleClient(CustomPacketPayload msg, Supplier<NetworkEvent.Context> ctx)
    {
    	Minecraft mc = Minecraft.getInstance();
    	LocalPlayer player = mc.player;
        if (mc.level != null && ClientState.hasLevelChanged(mc.level))
        {
            ClientState.onLevelChanged(mc.level);
        }
        
        if (msg != null)
        {
            switch (msg.type)
            {
                case DebuggingGeometryData:
                    ClientState.addDebuggingGeometry((GeometryData)msg.data);
                    break;

                case DebuggingGeometryRenderMode:
                    /*String strMode = (String)msg.data;

                    if ("_DISABLE_".equals(strMode))
                    {
                        ClientState.dgEnabled = false;
                        return;
                    }

                    if (strMode != null)
                    {
                        WireframeRenderMode idx = null;
                        for (WireframeRenderMode mode : WireframeRenderMode.values()) {
                        	if (mode.name().equalsIgnoreCase(strMode))
                            {
                                idx = mode;
                                break;
                            }
                        }
                        
                        if (idx != null)
                        {
                            ClientState.dgRenderingMode = idx;
                        }
                        else
                        {
                            player.sendMessage(new StringTextComponent("\u00a7cError: Invalid wireframe mode '" + strMode + "'"));
                        }
                    }
                    else*/
                    {
                        int mode = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
                        mode = (mode + 1) % WireframeRenderMode.values().length;
                        ClientState.dgRenderingMode = WireframeRenderMode.values()[mode];
                    }
                    
                    player.sendSystemMessage(MutableComponent.create(new LiteralContents("COG Client wireframe mode: " + ClientState.dgRenderingMode.name())));
                    break;

                case DebuggingGeometryReset:
                    ClientState.clearDebuggingGeometry();
                    break;

                case CommandResponse:
                    //player.sendMessage(new StringTextComponent((String)msg.data));
                    break;

                default:
                    throw new RuntimeException("Unhandled client packet type " + msg.type);
            }
        }
    }

	private static void handleServer(CustomPacketPayload msg, Supplier<NetworkEvent.Context> ctx)
    {
    	ServerPlayer player = ctx.get().getSender();
    	ServerLevel handlerLevel = player.getLevel();
        ServerState.checkIfServerChanged(handlerLevel.getServer(), handlerLevel.getServer().getWorldData());
        
        if (msg != null)
        {
            switch (msg.type)
            {
                case DebuggingGeometryRequest:
                    GeometryData geometryData = null;

                    if (player.getServer().getPlayerList().isOp(player.getGameProfile()));
                    {
                        geometryData = ServerState.getDebuggingGeometryData((GeometryRequestData)msg.data);
                    }

                    if (geometryData == null)
                    {
                        //sendTo(new CustomPacketPayload(PayloadType.DebuggingGeometryRenderMode, "_DISABLE_"), player);
                    }
                    else
                    {
                        sendTo(new CustomPacketPayload(PayloadType.DebuggingGeometryData, geometryData), player);
                    }

                    break;

                default:
                    throw new RuntimeException("Unhandled server packet type " + msg.type);
            }
        }
    }
	
	/**
	 * Sends a packet to the server.<br>
	 * Must be called Client side. 
	 */
	public static void sendToServer(Object msg)
	{
		//CHANNEL.sendToServer(msg);
	}
	
	/**
	 * Send a packet to a specific player.<br>
	 * Must be called Server side. 
	 */
	public static void sendTo(Object msg, ServerPlayer player)
	{
		/*if (!(player instanceof FakePlayer))
		{
			CHANNEL.sendTo(msg, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
		}*/
	}

	public static void sendToAllClients(Object msg) {
		//CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
	}
}
