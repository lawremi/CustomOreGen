package CustomOreGen;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import CustomOreGen.CustomPacketPayload.PayloadType;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.ClientState.WireframeRenderMode;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
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
