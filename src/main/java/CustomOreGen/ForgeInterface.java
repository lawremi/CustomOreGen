package CustomOreGen;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
        ClientState.onRenderWorld(Minecraft.getMinecraft().getRenderViewEntity(), (double)event.getPartialTicks());
    }

    @SubscribeEvent
    public void onLoadWorld(WorldEvent.Load event)
    {
    	World world = event.getWorld();
        if (world instanceof WorldServer)
        {
            ServerState.checkIfServerChanged(world.getMinecraftServer(), world.getWorldInfo());
            ServerState.getWorldConfig(world);
        }
        else if (event.getWorld() instanceof WorldClient && ClientState.hasWorldChanged(world))
        {
            ClientState.onWorldChanged(world);
        }
    }

    @SubscribeEvent
    public void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
    
    @SubscribeEvent
    public void onGenerateMinable(OreGenEvent.GenerateMinable event)
    {
    	World world = event.getWorld();
    	ServerState.checkIfServerChanged(world.getMinecraftServer(), world.getWorldInfo());
        boolean vanillaOreGen = ServerState.getWorldConfig(world).vanillaOreGen;
        boolean isCustom = event.getType() == OreGenEvent.GenerateMinable.EventType.CUSTOM;
        boolean isOre = event.getType() != OreGenEvent.GenerateMinable.EventType.GRAVEL && 
        		        event.getType() != OreGenEvent.GenerateMinable.EventType.DIRT &&
        		        // TODO: remove after we add stone generation to the configs
        		        event.getType() != OreGenEvent.GenerateMinable.EventType.ANDESITE &&
        		        event.getType() != OreGenEvent.GenerateMinable.EventType.DIORITE &&
        		        event.getType() != OreGenEvent.GenerateMinable.EventType.GRANITE; 
        event.setResult((vanillaOreGen || isCustom || !isOre) ? Result.ALLOW : Result.DENY);
    }
    
    @SubscribeEvent
    public void onForceChunk(ForceChunkEvent event) {
    	ServerState.chunkForced(event.getTicket().world, event.getLocation());
    }
    
    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiCreateWorld)
        {
            ServerState.addOptionsButtonToGui((GuiCreateWorld)event.getGui(), event.getButtonList());
        }
    }
    
    public static String getWorldDimensionFolder(World world)
    {
        return world.provider.getSaveFolder();
    }
}
