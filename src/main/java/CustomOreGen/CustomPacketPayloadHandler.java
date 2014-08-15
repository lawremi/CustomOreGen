package CustomOreGen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CustomPacketPayloadHandler {
	public CustomPacketPayloadHandler() {
		
	}
	
	@SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void clientCustomPayload(ClientCustomPacketEvent event)
    {
    	Minecraft mc = Minecraft.getMinecraft();
    	EntityClientPlayerMP player = mc.thePlayer;
        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
        }

        CustomPacketPayload payload = CustomPacketPayload.decodePacket(event.packet);

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
                            player.addChatMessage(new ChatComponentText("\u00a7cError: Invalid wireframe mode '" + strMode + "'"));
                        }
                    }
                    else
                    {
                        int var11 = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
                        var11 = (var11 + 1) % WireframeRenderMode.values().length;
                        ClientState.dgRenderingMode = WireframeRenderMode.values()[var11];
                    }

                    player.addChatMessage(new ChatComponentText("COG Client wireframe mode: " + ClientState.dgRenderingMode.name()));
                    break;

                case DebuggingGeometryReset:
                    ClientState.clearDebuggingGeometry();
                    break;

                case CommandResponse:
                    player.addChatMessage(new ChatComponentText((String)payload.data));
                    break;

                default:
                    throw new RuntimeException("Unhandled client packet type " + payload.type);
            }
        }
    }

	@SubscribeEvent
    public void serverCustomPayload(ServerCustomPacketEvent event)
    {
    	EntityPlayerMP player = ((NetHandlerPlayServer)event.handler).playerEntity;
    	World handlerWorld = player == null ? null : player.worldObj;
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), handlerWorld == null ? null : handlerWorld.getWorldInfo());
        CustomPacketPayload payload = CustomPacketPayload.decodePacket(event.packet);

        if (payload != null)
        {
            switch (payload.type)
            {
                case DebuggingGeometryRequest:
                    GeometryData geometryData = null;

                    if (player.mcServer.getConfigurationManager().func_152596_g(player.getGameProfile()));
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
