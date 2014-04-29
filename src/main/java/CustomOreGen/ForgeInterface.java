package CustomOreGen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ForgeInterface
{
    public static ForgeInterface createAndRegister()
    {
        CustomOreGenBase.log.debug("Registering Forge interface ...");
        ForgeInterface inst = new ForgeInterface();
        MinecraftForge.EVENT_BUS.register(inst);
        MinecraftForge.ORE_GEN_BUS.register(inst);
        //Enable when ready
        //MinecraftForge.EVENT_BUS.register(MystcraftObserver.instance());
        return inst;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        ClientState.onRenderWorld(Minecraft.getMinecraft().renderViewEntity, (double)event.partialTicks);
    }

    @SubscribeEvent
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

    @SubscribeEvent
    public void onGenerateMinable(OreGenEvent.GenerateMinable event)
    {
    	ServerState.checkIfServerChanged(MinecraftServer.getServer(), event.world.getWorldInfo());
        boolean vanillaOreGen = ServerState.getWorldConfig(event.world).vanillaOreGen;
        boolean isCustom = event.type == OreGenEvent.GenerateMinable.EventType.CUSTOM;
        boolean isOre = event.type != OreGenEvent.GenerateMinable.EventType.GRAVEL && 
        		        event.type != OreGenEvent.GenerateMinable.EventType.DIRT; 
        event.setResult((vanillaOreGen || isCustom || !isOre) ? Result.ALLOW : Result.DENY);
    }
    
    @SubscribeEvent
    public void onForceChunk(ForceChunkEvent event) {
    	ServerState.chunkForced(event.ticket.world, event.location);
    }
    
    public static String getWorldDimensionFolder(World world)
    {
        return world.provider.getSaveFolder();
    }
}
