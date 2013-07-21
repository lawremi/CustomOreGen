package CustomOreGen;

import java.util.Iterator;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.BaseMod;
import net.minecraft.src.ModLoader;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class mod_CustomOreGen extends BaseMod
{
    public String getVersion()
    {
        return "@VERSION@";
    }

    public String getPriorities()
    {
        return "after:*;";
    }

    public void load()
    {
        if (!CustomOreGenBase.hasFML())
        {
            CustomOreGenBase.log = ModLoader.getLogger();
        }

        if (!CustomOreGenBase.hasFML())
        {
            ModLoader.setInGameHook(this, true, false);
        }

        CustomPacketPayload.registerChannels(this);
    }

    public void modsLoaded()
    {
        CustomOreGenBase.onModPostLoad();
        boolean found = false;
        String failMods = null;
        for (BaseMod mod : ModLoader.getLoadedMods()) {
        	if (mod == this)
            {
                found = true;
            }
            else if (found && mod != null)
            {
                failMods = (failMods == null ? "" : failMods + ", ") + mod.getName();
            }
        }
        
        if (failMods == null)
        {
            CustomOreGenBase.log.finer("Confirmed that CustomOreGen has precedence during world generation");
        }
        else
        {
            CustomOreGenBase.log.warning("The following mods force ModLoader to load them *after* CustomOreGen: " + failMods + ".  Distributions may not behave as expected if they (1) target custom biomes from or (2) replace ores placed by these mods.");
        }
    }

    public void generateSurface(World world, Random rand, int blockX, int blockZ)
    {
        if (!CustomOreGenBase.hasFML())
        {
            ServerState.checkIfServerChanged(MinecraftServer.getServer(), world.getWorldInfo());
            ServerState.onPopulateChunk(world, rand, blockX / 16, blockZ / 16);
        }
    }

    public void generateNether(World world, Random rand, int blockX, int blockZ)
    {
        this.generateSurface(world, rand, blockX, blockZ);
    }

    @SideOnly(Side.CLIENT)
    public boolean onTickInGame(float partialTick, Minecraft minecraft)
    {
        if (CustomOreGenBase.hasFML())
        {
            return false;
        }
        else
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (mc.isSingleplayer())
            {
                ServerState.checkIfServerChanged(MinecraftServer.getServer(), (WorldInfo)null);
            }

            if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
            {
                ClientState.onWorldChanged(mc.theWorld);
            }

            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    public void clientCustomPayload(NetClientHandler handler, Packet250CustomPayload packet)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld != null && ClientState.hasWorldChanged(mc.theWorld))
        {
            ClientState.onWorldChanged(mc.theWorld);
        }

        CustomPacketPayload payload = CustomPacketPayload.decodePacket(packet);

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

                    if (!CustomOreGenBase.hasForge())
                    {
                        handler.handleChat(new Packet3Chat("{text: \"\u00a7cWarning: Minecraft Forge must be installed to view wireframes.\""));
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
                            handler.handleChat(new Packet3Chat("{text: \"\u00a7cError: Invalid wireframe mode \'" + strMode + "\'\""));
                        }
                    }
                    else
                    {
                        int var11 = ClientState.dgRenderingMode == null ? 0 : ClientState.dgRenderingMode.ordinal();
                        var11 = (var11 + 1) % WireframeRenderMode.values().length;
                        ClientState.dgRenderingMode = WireframeRenderMode.values()[var11];
                    }

                    handler.handleChat(new Packet3Chat("{text: \"COG Client wireframe mode: " + ClientState.dgRenderingMode.name() + "\"}"));
                    break;

                case DebuggingGeometryReset:
                    ClientState.clearDebuggingGeometry();
                    break;

                case MystcraftSymbolData:
                    if (!mc.isSingleplayer())
                    {
                        ClientState.addMystcraftSymbol((MystcraftSymbolData)payload.data);
                    }

                    break;

                case CommandResponse:
                    mc.ingameGUI.getChatGUI().printChatMessage((String)payload.data);
                    break;

                default:
                    throw new RuntimeException("Unhandled client packet type " + payload.type);
            }
        }
    }

    public void serverCustomPayload(NetServerHandler handler, Packet250CustomPayload packet)
    {
        World handlerWorld = handler.playerEntity == null ? null : handler.playerEntity.worldObj;
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), handlerWorld == null ? null : handlerWorld.getWorldInfo());
        CustomPacketPayload payload = CustomPacketPayload.decodePacket(packet);

        if (payload != null)
        {
            switch (payload.type)
            {
                case DebuggingGeometryRequest:
                    GeometryData geometryData = null;

                    if (handler.getPlayer().mcServer.getConfigurationManager().areCommandsAllowed(handler.getPlayer().username))
                    {
                        geometryData = ServerState.getDebuggingGeometryData((GeometryRequestData)payload.data);
                    }

                    if (geometryData == null)
                    {
                        (new CustomPacketPayload(PayloadType.DebuggingGeometryRenderMode, "_DISABLE_")).sendToClient(handler);
                    }
                    else
                    {
                        (new CustomPacketPayload(PayloadType.DebuggingGeometryData, geometryData)).sendToClient(handler);
                    }

                    break;

                default:
                    throw new RuntimeException("Unhandled server packet type " + payload.type);
            }
        }
    }

    public void onClientLogin(net.minecraft.entity.player.EntityPlayer player)
    {
        World handlerWorld = player == null ? null : player.worldObj;
        ServerState.checkIfServerChanged(MinecraftServer.getServer(), handlerWorld == null ? null : handlerWorld.getWorldInfo());

        if (player != null)
        {
            ServerState.onClientLogin((EntityPlayerMP)player);
        }
    }
}
