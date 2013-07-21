package CustomOreGen;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent.Load;

public class ForgeInterface
{
    public static ForgeInterface createAndRegister()
    {
        CustomOreGenBase.log.finer("Registering Forge interface ...");
        ForgeInterface inst = new ForgeInterface();
        MinecraftForge.EVENT_BUS.register(inst);
        return inst;
    }

    @ForgeSubscribe
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        ClientState.onRenderWorld(Minecraft.getMinecraft().renderViewEntity, (double)event.partialTicks);
    }

    @ForgeSubscribe
    public void onLoadWorld(Load event)
    {
        if (event.world instanceof WorldServer)
        {
            ServerState.checkIfServerChanged(MinecraftServer.getServer(), event.world.getWorldInfo());
            ServerState.getWorldConfig(event.world);
        }
        else if (event.world instanceof WorldClient && ClientState.hasWorldChanged(event.world))
        {
            ClientState.onWorldChanged(event.world);
        }
    }

    public static String getWorldDimensionFolder(World world)
    {
        return world.provider.getSaveFolder();
    }
}
