package CustomOreGen;

import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {
		
	}
	
	@SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void clientCustomPayload(ClientCustomPacketEvent event)
    {
    	Minecraft mc = Minecraft.getMinecraft();
    	EntityPlayerSP player = mc.thePlayer;
        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
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
                            player.addChatMessage(new TextComponentString("\u00a7cError: Invalid wireframe mode '" + strMode + "'"));
                        }
                    }
                    else
                    {
                        int mode = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
                        mode = (mode + 1) % WireframeRenderMode.values().length;
                        ClientState.dgRenderingMode = WireframeRenderMode.values()[mode];
                    }

                    player.addChatMessage(new TextComponentString("COG Client wireframe mode: " + ClientState.dgRenderingMode.name()));
                    break;

                case DebuggingGeometryReset:
                    ClientState.clearDebuggingGeometry();
                    break;

                case CommandResponse:
                    player.addChatMessage(new TextComponentString((String)payload.data));
                    break;

                default:
                    throw new RuntimeException("Unhandled client packet type " + payload.type);
            }
        }
    }

	@SubscribeEvent
    public void serverCustomPayload(ServerCustomPacketEvent event)
    {
    	EntityPlayerMP player = ((NetHandlerPlayServer)event.getHandler()).playerEntity;
    	World handlerWorld = player.worldObj;
        ServerState.checkIfServerChanged(handlerWorld.getMinecraftServer(), handlerWorld.getWorldInfo());
        CustomPacketPayload payload = CustomPacketPayload.decodePacket(event.getPacket());

        if (payload != null)
        {
            switch (payload.type)
            {
                case DebuggingGeometryRequest:
                    GeometryData geometryData = null;

                    if (player.mcServer.getPlayerList().canSendCommands(player.getGameProfile()));
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
