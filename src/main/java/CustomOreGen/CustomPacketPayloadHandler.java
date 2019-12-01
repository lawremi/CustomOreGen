package CustomOreGen;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {
		
	}
	
	@OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void clientCustomPayload(ClientCustomPacketEvent event)
    {
    	Minecraft mc = Minecraft.getInstance();
    	ClientPlayerEntity player = mc.player;
        if (mc.world != null && ClientState.hasWorldChanged(mc.world))
        {
            ClientState.onWorldChanged(mc.world);
        }

        CustomPacketPayload payload = CustomPacketPayload.decodePacket(event.getPacket());

        if (payload != null)
        {
            switch (payload.type)
            {
                case DebuggingGeometryData:
                    ClientState.addDebuggingGeometry((GeometryData)payload.data);
                    break;

                case DebuggingGeometryRenderMode:
                    String strMode = (String)payload.data;

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
                    else
                    {
                        int mode = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
                        mode = (mode + 1) % WireframeRenderMode.values().length;
                        ClientState.dgRenderingMode = WireframeRenderMode.values()[mode];
                    }

                    player.sendMessage(new StringTextComponent("COG Client wireframe mode: " + ClientState.dgRenderingMode.name()));
                    break;

                case DebuggingGeometryReset:
                    ClientState.clearDebuggingGeometry();
                    break;

                case CommandResponse:
                    player.sendMessage(new StringTextComponent((String)payload.data));
                    break;

                default:
                    throw new RuntimeException("Unhandled client packet type " + payload.type);
            }
        }
    }

	@SubscribeEvent
    public void serverCustomPayload(ServerCustomPacketEvent event)
    {
    	ServerPlayerEntity player = ((NetHandlerPlayServer)event.getHandler()).player;
    	World handlerWorld = player.world;
        ServerState.checkIfServerChanged(handlerWorld.getServer(), handlerWorld.getWorldInfo());
        CustomPacketPayload payload = CustomPacketPayload.decodePacket(event.getPacket());

        if (payload != null)
        {
            switch (payload.type)
            {
                case DebuggingGeometryRequest:
                    GeometryData geometryData = null;

                    if (player.server.getPlayerList().canSendCommands(player.getGameProfile()));
                    {
                        geometryData = ServerState.getDebuggingGeometryData((GeometryRequestData)payload.data);
                    }

                    if (geometryData == null)
                    {
                        (new CustomPacketPayload(PayloadType.DebuggingGeometryRenderMode, "_DISABLE_")).sendToClient(player);
                    }
                    else
                    {
                        (new CustomPacketPayload(PayloadType.DebuggingGeometryData, geometryData)).sendToClient(player);
                    }

                    break;

                default:
                    throw new RuntimeException("Unhandled server packet type " + payload.type);
            }
        }
    }
}
